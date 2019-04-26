import $ from 'jquery'
import 'chosen-js';
import 'bootstrap/js/tab'

(function() {

  /**
   * when a anchor is defined in the url (#)
   * then it will be opend.
   */
  function activateTabFromAnchorLink() {
    var url = window.location.href;
    var tabName = url.split('#')[1];
    if (tabName) {
      activaTab(tabName);
    }
  }

  function activaTab(tab) {
    $('.nav-tabs a[href="#' + tab + '"]').tab('show');
  }


  /**
   * updates config section 'Kalendar Sync'
   * shows dependent on Kalenderanbindung
   *   * Anbindung an Google Kalender
   *   * Anbindung an Microsoft Exchange Kalender
   */
  function updateVisibiltyCalendar() {
    const calenderSettingsProvider = document.querySelector('#calendarSettingsProvider');
    var value = calenderSettingsProvider ? calenderSettingsProvider.value : '';

    const googleCalendar = document.querySelector('#google-calendar');
    if (googleCalendar) {
      googleCalendar.hidden = value !== 'GoogleCalendarSyncProvider';
    }

    const exchangeCalendar = document.querySelector('#exchange-calendar');
    if (exchangeCalendar) {
      exchangeCalendar.hidden = value !== 'ExchangeCalendarProvider';
    }

    if (value === 'ExchangeCalendarProvider') {
      // problem if div is not displayed
      // https://github.com/harvesthq/chosen/issues/92
      $(".chosenCombo").chosen({width: "100%"});
    } else {
      // if not visible deactivate
      $(".chosenCombo").chosen('destroy');
    }
  }

  $(document).ready(function () {
    activateTabFromAnchorLink();

    // initial run to update view
    updateVisibiltyCalendar();

    const calendarSettingsProvider = document.querySelector('#calendarSettingsProvider');
    if (calendarSettingsProvider) {
      calendarSettingsProvider.addEventListener('change', () => {
        updateVisibiltyCalendar();
      })
    }
  });

})();
