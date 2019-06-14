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
package com.android.tools.idea.sqlite.controllers

import com.android.annotations.concurrency.UiThread
import com.android.tools.idea.concurrent.FutureCallbackExecutor
import com.android.tools.idea.device.fs.DeviceFileId
import com.android.tools.idea.sqlite.SqliteService
import com.android.tools.idea.sqlite.model.SqliteModel
import com.android.tools.idea.sqlite.model.SqliteModelListener
import com.android.tools.idea.sqlite.model.SqliteResultSet
import com.android.tools.idea.sqlite.model.SqliteSchema
import com.android.tools.idea.sqlite.model.SqliteTable
import com.android.tools.idea.sqlite.ui.SqliteEditorViewFactory
import com.android.tools.idea.sqlite.ui.mainView.SqliteView
import com.android.tools.idea.sqlite.ui.mainView.SqliteViewListener
import com.android.tools.idea.sqlite.ui.sqliteEvaluator.SqliteEvaluatorViewListener
import com.google.common.util.concurrent.FutureCallback
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.intellij.util.concurrency.EdtExecutorService
import java.util.concurrent.Executor

/**
 * Implementation of the application logic related to viewing/editing of a single sqlite database.
 *
 * All methods are assumed to run on the UI (EDT) thread.
 */
@UiThread
class SqliteController(
  parentDisposable: Disposable,
  private val viewFactory: SqliteEditorViewFactory,
  private val sqliteModel: SqliteModel,
  private val sqliteView: SqliteView,
  private val sqliteService: SqliteService,
  edtExecutor: EdtExecutorService,
  taskExecutor: Executor
) : Disposable {
  companion object {
    private val logger = Logger.getInstance(SqliteController::class.java)
  }

  private val edtExecutor = FutureCallbackExecutor.wrap(edtExecutor)
  private val taskExecutor = FutureCallbackExecutor.wrap(taskExecutor)

  private var sqliteEvaluatorController: SqliteEvaluatorController? = null

  /**
   * Controllers for all open result views, keyed by table name.
   *
   * <p>Multiple tables can be open at the same time in different tabs. This map keeps track of corresponding controllers.
   */
  private val resultSetControllers = mutableMapOf<String, ResultSetController>()

  private val sqliteViewListener = SqliteViewListenerImpl()
  private val sqliteModelListener = SqliteModelListenerImpl()

  init {
    Disposer.register(parentDisposable, this)
  }

  fun setUp() {
    sqliteView.addListener(sqliteViewListener)
    sqliteModel.addListener(sqliteModelListener)

    sqliteView.setUp()
    sqliteView.startLoading("Opening Sqlite database...")
    loadDbSchema()
  }

  override fun dispose() {
    sqliteView.removeListener(sqliteViewListener)
    sqliteModel.removeListener(sqliteModelListener)
  }

  private fun loadDbSchema() {
    val futureSchema = taskExecutor.transformAsync(sqliteService.openDatabase()) { sqliteService.readSchema() }

    edtExecutor.addListener(futureSchema) {
      if (!Disposer.isDisposed(this@SqliteController)) {
        sqliteView.stopLoading()
      }
    }

    edtExecutor.addCallback(futureSchema, object : FutureCallback<SqliteSchema> {
      override fun onSuccess(result: SqliteSchema?) {
        result?.let(::setDatabaseSchema)
      }

      override fun onFailure(t: Throwable) {
        if (!Disposer.isDisposed(this@SqliteController)) {
          sqliteView.reportErrorRelatedToService(sqliteService, "Error opening Sqlite database", t)
        }
      }
    })
  }

  private fun setDatabaseSchema(schema: SqliteSchema) {
    if (!Disposer.isDisposed(this)) {
      sqliteModel.schema = schema
    }
  }

  private fun updateView() {
    edtExecutor.addCallback(sqliteService.readSchema(), object : FutureCallback<SqliteSchema> {
      override fun onSuccess(schema: SqliteSchema?) {
        schema?.let { setDatabaseSchema(it) }
      }

      override fun onFailure(t: Throwable) {
        // TODO(b/132943925)
      }
    })
  }

  private inner class SqliteViewListenerImpl : SqliteViewListener {
    override fun tableNodeActionInvoked(table: SqliteTable) {
      if(resultSetControllers.containsKey(table.name)) {
        sqliteView.focusTable(table.name)
        return
      }

      edtExecutor.addCallback(sqliteService.readTable(table), object : FutureCallback<SqliteResultSet> {
        override fun onSuccess(sqliteResultSet: SqliteResultSet?) {
          if (sqliteResultSet != null) {

            val tableView = viewFactory.createTableView()
            sqliteView.displayTable(table.name, tableView.component)

            val resultSetController = ResultSetController(
              this@SqliteController,
              tableView, table.name, sqliteResultSet,
              edtExecutor
            ).also { it.setUp() }

            resultSetControllers[table.name] = resultSetController
          }
        }

        override fun onFailure(t: Throwable) {
          sqliteView.reportErrorRelatedToService(sqliteService, "Error reading Sqlite table \"${table.name}\"", t)
        }
      })
    }

    override fun closeTableActionInvoked(tableName: String) {
      sqliteView.closeTable(tableName)

      val controller = resultSetControllers.remove(tableName)
      controller?.let(Disposer::dispose)
    }

    override fun openSqliteEvaluatorActionInvoked() {
      if(sqliteEvaluatorController != null) {
        sqliteEvaluatorController?.requestFocus()
        return
      }

      val sqlEvaluatorView = viewFactory.createEvaluatorDialog()

      sqliteEvaluatorController = SqliteEvaluatorController(
        this@SqliteController,
        sqlEvaluatorView, sqliteService, edtExecutor
      ).also { it.setUp() }

      sqlEvaluatorView.addListener(SqliteEvaluatorViewListenerImpl())
    }
  }

  private inner class SqliteEvaluatorViewListenerImpl : SqliteEvaluatorViewListener {
    override fun evaluateSqlActionInvoked(sqlInstruction: String) {
      updateView()
    }

    override fun sessionClosed() {
      sqliteEvaluatorController = null
    }
  }

  private inner class SqliteModelListenerImpl : SqliteModelListener {
    override fun schemaChanged(schema: SqliteSchema) {
      logger.info("Schema changed $schema")
      sqliteView.displaySchema(schema)
    }

    override fun deviceFileIdChanged(fileId: DeviceFileId) {
      logger.info("Device file id changed: $fileId")
      //TODO(b/131588252)
    }
  }
}