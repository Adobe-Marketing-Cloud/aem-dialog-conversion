(function(window, hobs) {
  'use strict';

  var CONSOLE_RELATIVE_URL = "/libs/cq/dialogconversion/content/console.html";
  var SAMPLE_PATH = "/libs/cq/dialogconversion/content/sample";
  var SAMPLE_DIALOG_COUNT = 3;

  window.testsuites = window.testsuites || {};
  window.testsuites.cq = window.testsuites.cq || {};

  var commons = window.testsuites.wcm.commons = window.testsuites.wcm.commons || {};

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

  var aem_steps = hobs.steps.aem.commons;
  var i18n = {
    "found": "Found",
    "dialogs": "dialog(s)"
  };

  var beforeTest = new hobs.TestCase("Before Dialog Conversion Test")
    .navigateTo(CONSOLE_RELATIVE_URL);

  var suite = new hobs.TestSuite("Dialog Conversion", {
    execInNewWindow: true,
    execAfter: new hobs.TestCase('After suite')
      .execTestCase(aem_steps.resetDefaultUserPreferences),
    execBefore: new hobs.TestCase('Before suite')
      .execTestCase(aem_steps.setDefaultUserPreferences)
      .execSyncFct(function(opts) {
        hobs.utils.loadI18nParams(i18n, commons.config.locale);
      }),
    locale: commons.config.locale,
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
  );

}(window, window.hobs));
