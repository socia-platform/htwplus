$(document).ready(function () {
    /********************************************************************
     * New notification WebSocket system                                *
     ********************************************************************/
    /**
     * If true, various debug information is printed to console.
     *
     * @type {boolean}
     */
    var wsDebug = true;

    /**
     * Returns the correct WebSocket target URL respecting un-/encrypted context.
     *
     * @returns {string}
     */
    function getWsNotificationUrl() {
        return window.location.protocol === 'https:'
            ? 'wss://' + window.location.host + '/websocket/notification'
            : 'ws://' + window.location.host + '/websocket/notification';
    }

    /**
     * Initiates the notification WebSocket channel.
     */
    function wsNotificationInit() {
        if (wsDebug) {
            console.log('Initiate Notification WS');
        }

        var ws = window['MozWebSocket'] ? MozWebSocket : WebSocket;
        var notificationWebSocket = new ws(getWsNotificationUrl());
        notificationWebSocket.onmessage = function(event) { wsNotificationOnMessage(event) };
        notificationWebSocket.onerror = function(event) { wsNotificationOnError(event) };
    }

    /**
     * Creates a new li element with a new notification.
     *
     * @param notification
     * @returns {HTMLElement}
     */
    function createNotificationElement(notification) {
        var notificationElement = document.createElement('li');
        var parentDiv = document.createElement('div');
        var notificationLink = document.createElement('a');

        notificationElement.className = 'notification-element ' + (notification.is_read ? 'read' : 'unread');
        notificationElement.id = 'notification_' + notification.id;
        notificationElement.style.display = 'none';
        notificationElement.appendChild(parentDiv);
        notificationLink.href = '/notification/' + notification.id;
        notificationLink.innerHTML = notification.content;
        parentDiv.appendChild(notificationLink);

        return notificationElement;
    }

    /**
     * Updates the notifications, if necessary.
     *
     * @param notifications
     */
    function updateNotifications(notifications) {
        // create new elements for each new notification, append them before last list element (like "show all notifications")
        for (var notificationIndex in notifications) {
            if (notifications.hasOwnProperty(notificationIndex)) {
                var notification = notifications[notificationIndex];
                var notificationListElement = $('#notification_' + notification.id);

                // check, if the li element is already available
                if (notificationListElement.length) {
                    // li element available, check, if status changed
                    if (notificationListElement.hasClass((notification.is_read ? 'read' : 'unread'))) {
                        // status did not change, just go on...
                        continue;
                    } else {
                        // status changed, we must recreate this element with new content
                        notificationListElement.remove();
                    }
                }

                // li element not available, create new element and append
                var newNotificationElement = createNotificationElement(notification);
                $('#hp-notifications-item').find('li:first').before(newNotificationElement);
                $(newNotificationElement).fadeIn('slow');
            }
        }

        // update counter and eventually delete obsolete previous notifications
        updateNewNotificationCounter(notifications);
        deleteObsoleteNotifications(notifications);
    }

    /**
     * Updates the new notification counter.
     *
     * @param notifications
     */
    function updateNewNotificationCounter(notifications) {
        // count new/unread notifications
        var unreadNotifications = 0;
        for (var notificationIndex in notifications) {
            if (notifications.hasOwnProperty(notificationIndex)) {
                if (!notifications[notificationIndex].is_read) {
                    unreadNotifications++;
                }
            }
        }

        var notificationCounters = $('#hp-notifications-item').find('.badge');
        // if update counters
        for (var counterIndex = 0; counterIndex < notificationCounters.size(); counterIndex++) {
            if (notificationCounters.hasOwnProperty(counterIndex)) {
                notificationCounters[counterIndex].innerHTML = unreadNotifications;
                if (unreadNotifications > 0) {
                    // if counter is hidden, show it
                    if (notificationCounters[counterIndex].style.display == 'none') {
                        $(notificationCounters[counterIndex]).fadeIn('slow');
                    }
                } else {
                    // we have zero unread notifications, hide counter
                    if (notificationCounters[counterIndex].style.display != 'none') {
                        $(notificationCounters[counterIndex]).fadeOut('slow');
                    }
                }
            }
        }
    }

    /**
     * Deletes obsolete notifications, if number of opened notifications bigger than numbers of notifications.
     */
    function deleteObsoleteNotifications(notifications) {
        var notificationDropDownLayer = $('#hp-notifications-item');
        while (notificationDropDownLayer.find('li').length > notifications.length + 1) { // length + 1 because the last li element is no notification
            notificationDropDownLayer.find('li:nth-last-child(2)').remove();
        }
    }

    /**
     * Notification WebSocket on message listener. If wsDebug=true, WebSocket communication is logged.
     *
     * @param event
     */
    function wsNotificationOnMessage(event) {
        try {
            var notifications = JSON.parse(event.data);

            if (wsDebug) {
                console.log(notifications);
            }

            updateNotifications(notifications);

//            var setOpen = false;
//            if (notificationDropDownLayer.hasClass('open')) {
//                setOpen = true;
//            }
//
//            if (setOpen == true) {
//                notificationDropDownLayer.addClass('open');
//            }
        } catch (exception) {
            if (wsDebug) {
                console.log('Client exception: ' + exception);
                console.log('Data from server: ' + event.data);
            }
        }
    }

    /**
     * Notification WebSocket on error listener. Errors are logged on wsDebug=true only.
     *
     * @param event
     */
    function wsNotificationOnError(event) {
        if (wsDebug) {
            console.log('Error: ' + event.data);
        }
    }

    // now init the WebSocket
    wsNotificationInit();
});