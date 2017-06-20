/*
 *  (c) 2017 Adobe. All rights reserved.
 *  This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License. You may obtain a copy
 *  of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under
 *  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *  OF ANY KIND, either express or implied. See the License for the specific language
 *  governing permissions and limitations under the License.
 */
(function(window, hobs) {
  'use strict';

  var CONSOLE_RELATIVE_URL = "/libs/cq/dialogconversion/content/console.html";
  var SAMPLE_PATH = "/libs/cq/dialogconversion/content/sample";
  var SAMPLE_DIALOG_COUNT = 3;
  var TOKEN_SERVLET = "/libs/granite/csrf/token.json";
  var LOCALE_USER_DEFAULT = "en";
  var LOCALE_SUITE = "fr";

  window.testsuites = window.testsuites || {};
  window.testsuites.cq = window.testsuites.cq || {};

  var i18n = {
    "found": "Found",
    "dialogs": "dialog(s)"
  };

  var setPreferences = new hobs.TestCase("Set User Preferences")
    .execFct(function(opts, done) {
      // set language to config locale value
      var result = Granite.HTTP.eval(TOKEN_SERVLET);
      var data = hobs.param("preferences")(opts);
      data[":cq_csrf_token"] = result.token;
      jQuery.post( hobs.config.context_path + hobs.utils.getUserInfo().home(opts)+"/preferences", data)
        .always(function() {
          done();
        });
    });

  var beforeSuite = new hobs.TestCase("Before Suite")
    .execFct(function(opts, done) {
      var t = new hobs.TestCase("", opts)
        .execTestCase(setPreferences, false, {
          params: {
            preferences: {
              "granite.shell.showonboarding620": false, // disable onboarding
              language: LOCALE_SUITE // set language
            }
          }
        });
      t.exec().then(function() {
        done();
      });
    })
    .execSyncFct(function(opts) {
      hobs.utils.loadI18nParams(i18n, LOCALE_SUITE);
    });

  var afterSuite = new hobs.TestCase("After Suite")
    .execTestCase(setPreferences, false, {
      params: {
        preferences: {
          "granite.shell.showonboarding620@Delete": true, // enable onboarding
          language: LOCALE_USER_DEFAULT } // reset language
      }
    });

  var selectors = {
    dialogSearch: {
      self: ".js-cq-DialogConverter-dialogSearch",
      searchPathField: ".js-cq-DialogConverter-searchPath",
      showDialogsButton: ".js-cq-DialogConverter-showDialogs",
      infoText: ".js-cq-DialogConverter-infoText",
      showConvertedDialogsCheckbox: ".js-cq-DialogConverter-showConvertedDialogRows input[type=checkbox]",
      convertDialogsButton: ".js-cq-DialogConverter-convertDialogs",
      dialogsTable: {
        self: ".js-cq-DialogConverter-dialogs",
        row: "[is=coral-table-row].foundation-collection-item",
        convertedRow: "[data-dialogconversion-dialog-converted]",
        selectAll: "[is=coral-table-headercell] input[type=checkbox]"
      }
    },
    results: {
      backButton: ".js-cq-DialogConverter-back",
      resultsTable: {
        self: ".js-cq-DialogConverter-convertedDialogs",
        row: "[is=coral-table-row].foundation-collection-item"
      }
    }
  };

  var beforeTest = new hobs.TestCase("Before Dialog Conversion Test")
    .navigateTo(CONSOLE_RELATIVE_URL);

  var suite = new hobs.TestSuite("Dialog Conversion", {
    execInNewWindow: true,
    execBefore: beforeSuite,
    execAfter: afterSuite,
    locale: LOCALE_SUITE,
    path: "/libs/cq/gui/tests/dialogconversion/dialogconversion.js",
    register: true
  });

  window.testsuites.cq.dialogconversion = window.testsuites.cq.dialogconversion || suite;

  function assertDialogCount(count) {
    var dialogs = hobs.find(selectors.dialogSearch.dialogsTable.row);
    return dialogs.length === count;
  }

  function assertSelectedDialogCount(count) {
    var dialogs = hobs.find(selectors.dialogSearch.dialogsTable.row + "[selected]");
    return dialogs.length === count;
  }

  function assertResultCount(count) {
    var dialogs = hobs.find(selectors.results.resultsTable.row);
    return dialogs.length === count;
  }

  /**
   * Test the search function for the sample dialogs
   *
   * 1. Search for sample dialogs
   * 2. Check the count of returned dialog list items
   * 3. Check that the info text is correctly updated
   */
  suite.addTestCase(new hobs.TestCase("Dialog search", { before: beforeTest })
    // 1
    .assert.visible(selectors.dialogSearch.searchPathField)
    .fillInput(selectors.dialogSearch.searchPathField, SAMPLE_PATH)
    .assert.exist(selectors.dialogSearch.showDialogsButton, true)
    .click(selectors.dialogSearch.showDialogsButton, { expectNav: true })

    // 2
    .assert.isTrue(function() {
      return assertDialogCount(SAMPLE_DIALOG_COUNT);
    })

    // 3
    .assert.exist(selectors.dialogSearch.infoText + ":contains(%i18n_found% " + SAMPLE_DIALOG_COUNT + " %i18n_dialogs%)", true)
  );

  /**
   * Test the selection function for the sample dialogs
   *
   * 1. Show sample dialogs
   * 2. Check the select all dialogs checkbox
   * 3. Verify selected row count
   * 4. Un-check a dialog row
   * 5. Verify selected row count
   */
  suite.addTestCase(new hobs.TestCase("Dialog selection", { before: beforeTest })
    // 1
    .navigateTo(CONSOLE_RELATIVE_URL + "?path=" + SAMPLE_PATH)
    .assert.isTrue(function() {
      return assertDialogCount(SAMPLE_DIALOG_COUNT);
    })

    // 2
    .assert.exist(selectors.dialogSearch.dialogsTable.selectAll, true)
    .click(selectors.dialogSearch.dialogsTable.selectAll)

    // 3
    .assert.isTrue(function() {
      return assertSelectedDialogCount(SAMPLE_DIALOG_COUNT);
    })

    // 4
    .assert.exist(selectors.dialogSearch.dialogsTable.row + "[selected]:eq(0) input[type=checkbox]", true)
    .click(selectors.dialogSearch.dialogsTable.row + "[selected]:eq(0) input[type=checkbox]")

    // 5
    .assert.isTrue(function() {
      return assertSelectedDialogCount(SAMPLE_DIALOG_COUNT - 1);
    })
  );

  /**
   * Test the dialog conversion and show converted dialogs feature
   *
   * 1. Show sample dialogs
   * 2. Check the select all dialogs checkbox
   * 3. Click to convert the dialogs
   * 4. Verify conversion UI
   * 5. Navigate back to search UI
   * 6. Test Show converted dialogs
   * 7. Verify the convert button remains hidden following a select all when there are no convertible dialogs
   */
  suite.addTestCase(new hobs.TestCase("Convert dialogs and show converted dialogs", { before: beforeTest })
    // 1
    .navigateTo(CONSOLE_RELATIVE_URL + "?path=" + SAMPLE_PATH)
    .assert.isTrue(function() {
      return assertDialogCount(SAMPLE_DIALOG_COUNT);
    })

    // 2
    .assert.exist(selectors.dialogSearch.dialogsTable.selectAll, true)
    .click(selectors.dialogSearch.dialogsTable.selectAll)

    // 3
    .assert.exist(selectors.dialogSearch.convertDialogsButton, true)
    // delayBefore to ensure Coral.Table is updated following selection
    .click(selectors.dialogSearch.convertDialogsButton, { delayBefore: 1000 })

    // 4
    .assert.visible(selectors.dialogSearch.dialogsTable.self, false)
    .assert.visible(selectors.results.resultsTable.self, true)
    .assert.visible(selectors.results.backButton, true)
    .assert.isTrue(function() {
      return assertResultCount(SAMPLE_DIALOG_COUNT);
    })

    // 5
    .click(selectors.results.backButton)

    // 6
    .assert.visible(selectors.dialogSearch.dialogsTable.self, true)
    .assert.visible(selectors.results.resultsTable.self, false)
    .assert.visible(selectors.dialogSearch.showConvertedDialogsCheckbox, true)
    // delayBefore to ensure table content is loaded before toggling
    .click(selectors.dialogSearch.showConvertedDialogsCheckbox, { delayBefore: 1500 })
    .assert.visible(selectors.dialogSearch.dialogsTable.convertedRow, true)

    // 7
    .assert.exist(selectors.dialogSearch.dialogsTable.selectAll, true)
    .click(selectors.dialogSearch.dialogsTable.selectAll)
    .assert.visible(selectors.dialogSearch.convertDialogsButton, false)
  );

}(window, window.hobs));
