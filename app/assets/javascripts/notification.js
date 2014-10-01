function WS() {
    this.debug = true;
    this.targetUrl = window.location.protocol === 'https:'
        ? 'wss://' + window.location.host + '/websocket'
        : 'ws://' + window.location.host + '/websocket';
    this.socket = window.MozWebSocket ? new window.MozWebSocket(this.targetUrl) : new window.WebSocket(this.targetUrl);

    var ws = this;

    /**
     * WebSocket on message listener. If this.debug=true, WebSocket communication is logged.
     *
     * @param e Event
     */
    this.socket.onmessage = function(e) {
        try {
            var data = JSON.parse(e.data);

            if (ws.debug) {
                console.log(data);
            }

            if (data.code == "OK" && data.method == "ReceiveNotification" && data.notification) {
                ws.updateNotifications([data.notification]);
            }
        } catch (exception) {
            if (ws.debug) {
                console.log('Client exception while receiving from WS: ' + exception);
                console.log('Data from server: ' + e.data);
            }
        }
    };

    /**
     * WebSocket on error listener. Errors are logged on this.debug=true only.
     *
     * @param e Event
     */
    this.socket.onerror = function(e) {
        if (ws.debug) {
            console.log('Error: ' + e.data);
        }
    };

    /**
     * Sends a message to the WebSocket.
     *
     * @param data The message to send
     */
    this.send = function(data) {
        if (ws.debug) {
            var sendingJson = JSON.stringify(data);
            console.log('WS Send: ' + sendingJson);
        }

        try {
            this.socket.send(sendingJson);
        } catch (exception) {
            if (ws.debug) {
                console.log('Client exception while sending to WS: ' + exception);
            }
        }
    };

    /**
     * Creates a new li element with a new notification.
     *
     * @param notification
     * @returns {HTMLElement}
     */
    this.createNotificationElement = function(notification) {
        var notificationElement = document.createElement('li');
        var notificationLink = document.createElement('a');

        notificationLink.href = '/notification/' + notification.id;
        notificationLink.innerHTML = notification.content;
        notificationElement.className = 'notification-element ' + (notification.is_read ? 'read' : 'unread');
        notificationElement.id = 'notification_' + notification.id;
        notificationElement.style.display = 'none';
        notificationElement.appendChild(notificationLink);

        return notificationElement;
    };

    /**
     * Updates the notifications, if necessary.
     *
     * @param notifications
     */
    this.updateNotifications = function(notifications) {
        // create new elements for each new notification, append them before last list element (like "show all notifications")
        for (var notificationIndex in notifications) {
            if (notifications.hasOwnProperty(notificationIndex)) {
                var notification = notifications[notificationIndex];
                var notificationListElement = $('#notification_' + notification.id);

                // check, if the li element is already available
                if (notificationListElement.length) {
                    // status changed, we must recreate this element with new content
                    notificationListElement.remove();
                }

                // li element not available, create new element and append
                var newNotificationElement = this.createNotificationElement(notification);
                $('#hp-notifications-item').find('li:first').before(newNotificationElement);
                $(newNotificationElement).fadeIn('slow');
            }
        }

        // update counter and eventually delete obsolete previous notifications
        this.updateNewNotificationCounter();
        this.deleteObsoleteNotifications();
    };

    /**
     * Updates the new notification counter.
     *
     * @param notifications
     */
    this.updateNewNotificationCounter = function() {
        // count new/unread notifications
        var unreadNotifications = $('.unread').length;

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
    };

    /**
     * Deletes obsolete notifications, if number of opened notifications bigger than numbers of notifications.
     */
    this.deleteObsoleteNotifications = function() {
        var maxLiElements = 11; // 11 because 10 notifications + last li element (show all)
        var notificationDropDownLayer = $('#hp-notifications-item');
        while (notificationDropDownLayer.find('li').length > maxLiElements) {
            notificationDropDownLayer.find('li:nth-last-child(2)').remove();
        }
        $('.notification-show-all').removeClass('hidden');
        $('.nothing-new').remove();
    };
}

var webSocket;
$(document).ready(function () {
    webSocket = new WS();
});