/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.res.psi

import com.android.SdkConstants
import com.android.SdkConstants.ATTR_NAME
import com.android.ide.common.rendering.api.ResourceNamespace
import com.android.ide.common.rendering.api.ResourceReference
import com.android.ide.common.resources.FileResourceNameValidator
import com.android.resources.FolderTypeRelationship
import com.android.resources.ResourceType
import com.android.resources.ResourceType.STYLEABLE
import com.android.resources.ResourceUrl
import com.android.tools.idea.model.Namespacing
import com.android.tools.idea.res.AndroidRClassBase
import com.android.tools.idea.res.ResourceRepositoryManager
import com.android.tools.idea.res.ResourceRepositoryRClass
import com.android.tools.idea.res.SmallAarRClass
import com.android.tools.idea.res.TransitiveAarRClass
import com.android.tools.idea.res.getFolderType
import com.android.tools.idea.res.getResourceTypeForResourceTag
import com.android.tools.idea.res.isInResourceSubdirectory
import com.android.tools.idea.res.isValueBased
import com.android.tools.idea.res.resolve
import com.android.tools.idea.res.resourceNamespace
import com.android.tools.idea.util.androidFacet
import com.android.utils.SdkUtils
import com.android.utils.reflection.qualifiedName
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.ElementDescriptionLocation
import com.intellij.psi.ElementDescriptionProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.FakePsiElement
import com.intellij.psi.impl.compiled.ClsFieldImpl
import com.intellij.psi.util.parentOfType
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import com.intellij.refactoring.rename.RenameHandler
import com.intellij.usageView.UsageViewLongNameLocation
import com.intellij.usageView.UsageViewTypeLocation
import icons.StudioIcons
import org.jetbrains.android.augment.ResourceLightField
import org.jetbrains.android.augment.StyleableAttrLightField
import javax.swing.Icon

/**
 * A fake PsiElement that wraps a [ResourceReference].
 *
 * Android resources can have multiple definitions, but most editor operations should not need to know how many definitions a given resource
 * has or which one was used to start a refactoring, e.g. a rename. A [ResourceReferencePsiElement] implements [PsiElement] so can be used
 * in editor APIs, but abstracts away how the resource was actually defined.
 *
 * Most [PsiReference]s related to Android resources resolve to instances of this class (other than R fields, since we don't control
 * references in Java/Kotlin). We do this instead of using [PsiPolyVariantReference] and resolving to all definitions, because checking if a
 * resource is defined at all can be done using resource repositories and is faster than determining the exact XML attribute that defines
 * it. Another reason is that most refactorings (e.g. renaming) are only passed one [PsiElement] as their "target". This class is used to
 * recognize all mentions of Android resources (XML, * "@+id", R fields) as early as possible. Custom [FindUsagesHandler],
 * [GotoDeclarationHandler] and [RenameHandler] are used to handle all these cases uniformly.
 */
class ResourceReferencePsiElement(
  val resourceReference: ResourceReference,
  val psiManager: PsiManager,
  val writable: Boolean = false) : FakePsiElement() {

  companion object {

    @JvmField val RESOURCE_ICON: Icon =  StudioIcons.Shell.ToolWindows.VISUAL_ASSETS
    @JvmField val RESOURCE_CONTEXT_ELEMENT: Key<PsiElement> = Key.create<PsiElement>(::RESOURCE_CONTEXT_ELEMENT.qualifiedName)

    @JvmStatic
    fun create(element: PsiElement): ResourceReferencePsiElement? {
      return when (element) {
        is ResourceReferencePsiElement -> element
        is ResourceLightField -> convertResourceLightField(element)
        is StyleableAttrLightField -> convertStyleableAttrLightField(element)
        is ClsFieldImpl -> convertClsFieldImpl(element)
        is XmlAttributeValue -> convertXmlAttributeValue(element)
        is PsiFile -> convertPsiFile(element)
        else -> null
      }
    }

    private fun convertPsiFile(element: PsiFile): ResourceReferencePsiElement? {
      if (!isInResourceSubdirectory(element)) {
        return null
      }
      val resourceFolderType = getFolderType(element) ?: return null
      val resourceType = FolderTypeRelationship.getNonIdRelatedResourceType(resourceFolderType)
      val resourceNamespace = element.resourceNamespace ?: return null
      if (FileResourceNameValidator.getErrorTextForFileResource(element.name, resourceFolderType) != null) return null
      val resourceName = SdkUtils.fileNameToResourceName(element.name)
      return ResourceReferencePsiElement(ResourceReference(resourceNamespace, resourceType, resourceName), element.manager)
    }

    private fun convertStyleableAttrLightField(element: StyleableAttrLightField): ResourceReferencePsiElement? {
      val grandClass = element.containingClass.containingClass as? AndroidRClassBase ?: return null
      val facet = element.androidFacet
      val namespacing = facet?.let { ResourceRepositoryManager.getInstance(it).namespacing }
      val resourceNamespace = if (Namespacing.REQUIRED == namespacing) {
        ResourceNamespace.fromPackageName(StringUtil.getPackageName(grandClass.qualifiedName!!))
      }
      else {
        ResourceNamespace.RES_AUTO
      }
      return ResourceReferencePsiElement(ResourceReference(resourceNamespace, STYLEABLE, element.name), element.manager)
    }

    private fun convertResourceLightField(element: ResourceLightField): ResourceReferencePsiElement? {
      val grandClass = element.containingClass.containingClass as? AndroidRClassBase ?: return null
      return when (grandClass) {
        is ResourceRepositoryRClass -> {
          val facet = element.androidFacet
          val namespacing = facet?.let { ResourceRepositoryManager.getInstance(it).namespacing }
          val resourceNamespace = if (Namespacing.REQUIRED == namespacing) {
            ResourceNamespace.fromPackageName(StringUtil.getPackageName(grandClass.qualifiedName!!))
          }
          else {
            ResourceNamespace.RES_AUTO
          }
          ResourceReferencePsiElement(ResourceReference(resourceNamespace, element.resourceType, element.resourceName), element.manager)
        }
        is TransitiveAarRClass -> {
          ResourceReferencePsiElement(ResourceReference(ResourceNamespace.RES_AUTO, element.resourceType, element.resourceName), element.manager)
        }
        is SmallAarRClass -> {
          val resourceNamespace = ResourceNamespace.fromPackageName(StringUtil.getPackageName(grandClass.qualifiedName!!))
          ResourceReferencePsiElement(ResourceReference(resourceNamespace, element.resourceType, element.resourceName), element.manager)
        }
        else -> null
      }
    }

    /**
     * This is for compiled framework resources in Java/Kotlin files.
     */
    private fun convertClsFieldImpl(element: ClsFieldImpl) : ResourceReferencePsiElement? {
      val containingClass = element.containingClass ?: return null
      if (containingClass.containingClass?.qualifiedName != SdkConstants.CLASS_R) {
        return null
      }
      val resourceType = containingClass.name?.let { ResourceType.fromClassName(it) } ?: return null
      return ResourceReferencePsiElement(ResourceReference(ResourceNamespace.ANDROID, resourceType, element.name), element.manager)
    }

    /**
     * Attempts to convert an XmlAttributeValue into ResourceReferencePsiElement, if the attribute references or defines a resources.
     *
     * These are cases where the [com.intellij.util.xml.ResolvingConverter] does not provide a PsiElement and so the dom provides the
     * invocation element.
     * TODO: Implement resolve in all ResolvingConverters so that we never get the underlying element.
     */
    private fun convertXmlAttributeValue(element: XmlAttributeValue) : ResourceReferencePsiElement? {
      val resUrl = ResourceUrl.parse(element.value)
      if (resUrl != null) {
        val resourceReference = resUrl.resolve(element) ?: return null
        return ResourceReferencePsiElement(resourceReference, element.manager)
      }
      else {
        // Instances of value resources
        val tag = element.parentOfType<XmlTag>() ?: return null
        if ((element.parent as XmlAttribute).name != ATTR_NAME) return null
        val type = getResourceTypeForResourceTag(tag) ?: return null
        val resourceReference = if (type == ResourceType.ATTR) {
          ResourceUrl.parseAttrReference(element.value)?.resolve(element) ?: return null
        } else {
          if (!type.isValueBased()) return null
          val name = element.value
          ResourceReference(ResourceNamespace.TODO(), type, name)
        }
        return ResourceReferencePsiElement(resourceReference, element.manager)
      }
    }
  }

  override fun getIcon(open: Boolean): Icon = RESOURCE_ICON

  override fun getPresentableText(): String? = resourceReference.resourceUrl.toString()

  override fun getManager(): PsiManager = psiManager

  override fun getProject() = psiManager.project

  override fun getParent(): PsiElement? = null

  override fun isValid() = true

  override fun isWritable(): Boolean {
    return writable
  }

  override fun getContainingFile(): PsiFile? = null

  override fun getName() = resourceReference.name

  override fun isEquivalentTo(element: PsiElement) = create(element) == this

  /**
   * Element description for Android resources.
   */
  class ResourceReferencePsiElementDescriptorProvider : ElementDescriptionProvider {
    override fun getElementDescription(element: PsiElement, location: ElementDescriptionLocation): String? {
      val resourceReference = (element as? ResourceReferencePsiElement)?.resourceReference ?: return null
      return when (location) {
        is UsageViewTypeLocation -> "${resourceReference.resourceType.displayName} Resource"
        is UsageViewLongNameLocation -> element.presentableText
        else -> resourceReference.name
      }
    }
  }

  override fun equals(other: Any?): Boolean {
    return (other as? ResourceReferencePsiElement)?.resourceReference == this.resourceReference
  }

  fun toWritableResourceReferencePsiElement() : ResourceReferencePsiElement? {
    // Framework resources are not writable.
    if (this.resourceReference.namespace != ResourceNamespace.ANDROID) {
      val writableElement = ResourceReferencePsiElement(this.resourceReference, this.psiManager, true)
      copyCopyableDataTo(writableElement)
      return writableElement
    }
    return null
  }

  override fun hashCode(): Int {
    var result = resourceReference.hashCode()
    result = 31 * result + psiManager.hashCode()
    result = 31 * result + writable.hashCode()
    return result
  }
}
