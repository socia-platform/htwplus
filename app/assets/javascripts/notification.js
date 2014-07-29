$(document).ready(function () {
    /********************************************************************
     * New notification WebSocket system                                *
     ********************************************************************/

    // returns the correct WebSocket target URL respecting un-/encrypted context
    function getWsNotificationUrl() {
        return window.location.protocol === 'https:'
            ? 'wss://' + window.location.host + '/websocket/notification'
            : 'ws://' + window.location.host + '/websocket/notification';
    }

    // initiates the notification WebSocket channel
    var wsDebug = true;
    function wsNotificationInit() {
        if (wsDebug) {
            console.log('Initiate Notification WS');
        }

        var ws = window['MozWebSocket'] ? MozWebSocket : WebSocket;
        var notificationWebSocket = new ws(getWsNotificationUrl());
        notificationWebSocket.onmessage = function(event) { wsNotificationOnMessage(event) };
        notificationWebSocket.onerror = function(event) { wsNotificationOnError(event) };
    }

    // notification WebSocket on message listener
    function wsNotificationOnMessage(event) {
        try {
            var notifications = JSON.parse(event.data);
            var notificationDropDownLayer = $('#hp-notifications-item');

            if (wsDebug) {
                console.log(notifications);
            }

            // create new elements for each new notification, append them before last list
            // element (like "show all notifications")
            for (var notificationIndex in notifications) {
                if (notifications.hasOwnProperty(notificationIndex)) {
                    var currentNotification = notifications[notificationIndex];
                    var notificationListElement = $('#notification_' + currentNotification.id);

                    // check, if the li element is already available
                    if (notificationListElement.length) {
                        // li element available, do nothing
                        continue;
                    }

                    // li element not available, create new li element and append
                    var newNotification = document.createElement('li');
                    newNotification.className = currentNotification.is_read ? 'read' : 'unread';
                    newNotification.innerHTML = '<div>' + currentNotification.content + '</div>';
                    newNotification.id = 'notification_' + currentNotification.id;
                    notificationDropDownLayer.find('li:first').before(newNotification);
                }
            }

            // if badges are available, increase counter
            var notificationCounters = notificationDropDownLayer.find('.badge');
            if (notificationCounters.length) {
                for (var counterIndex in notificationCounters) {
                    if (notificationCounters.hasOwnProperty(counterIndex)) {
                        notificationCounters[counterIndex].innerHTML = notifications.length;
                    }
                }
            }

            // delete obsolete notifications, if number of opened notifications bigger than numbers of new notifications
            while (notificationDropDownLayer.find('li').length > notifications.length + 1) {
                notificationDropDownLayer.find('li:nth-last-child(2)').remove();
            }

            var setOpen = false;
            if (notificationDropDownLayer.hasClass('open')) {
                setOpen = true;
            }

            if (setOpen == true) {
                notificationDropDownLayer.addClass('open');
            }
        } catch (exception) {
            if (wsDebug) {
                console.log('Client exception: ' + exception);
                console.log('Data from server: ' + event.data);
            }
        }
    }

    // notification WebSocket on error listener
    function wsNotificationOnError(event) {
        if (wsDebug) {
            console.log('Error: ' + event.data);
        }
    }

    // now init the WebSocket
    wsNotificationInit();
});