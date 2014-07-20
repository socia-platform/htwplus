$(document).ready(function () {
	
	function reloadNotifications()  {
		$.ajax({
			url: "/notification/view",
			type: "GET",
			success: function(data){
				var setOpen = false;
				if($("#hp-notification-item").hasClass("open")){
					setOpen = true;
				}
				$("#hp-notification-item").replaceWith(data);
				
				if(setOpen == true) {
					$("#hp-notification-item").addClass("open");
				}

			}
		});
	}

    function loadHistoryNotifications()  {
        $.ajax({
            url: "/notification/view_history",
            type: "GET",
            success: function(data){
                var setOpen = false;
                if($("#hp-notification-item").hasClass("open")){
                    setOpen = true;
                }
                $("#hp-notification-item").replaceWith(data);

                if(setOpen == true) {
                    $("#hp-notification-item").addClass("open");
                }

            }
        });
    }
	
	function deleteNotifications() {
		$.ajax({
			url: "/notification/delete",
			type: "GET",
			success: function(data){
				reloadNotifications(); 		
			}
		});
		
	}
	
	$('body').on("click", "#hp-delete-notifications", function(){
		deleteNotifications();
		return false;
	});

    $('body').on("click", "#hp-show-notification-history", function() {
            loadHistoryNotifications();
            return false;
        }
    );

    $('body').on("click", "#hp-show-notification-unread", function() {
            reloadNotifications();
            return false;
        }
    );
	
	setInterval( reloadNotifications, 30000 );

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
    function wsNotificationInit() {
        console.log('Initiate Notification WS');
        var ws = window['MozWebSocket'] ? MozWebSocket : WebSocket;
        var notificationWebSocket = new ws(getWsNotificationUrl());
        notificationWebSocket.onmessage = function(event) { wsNotificationOnMessage(event) };
        notificationWebSocket.onerror = function(event) { wsNotificationOnError(event) };
    }

    // notification WebSocket on message listener
    function wsNotificationOnMessage(event) {
        try {
            var notifications = JSON.parse(event.data);
            var notificationDropDownLayer = $('#hp-notification-item');

            // remove previous new notifications
            $('.new-notification').remove();

            // create new elements for each new notification, append them before last list
            // element (like "show all notifications")
            for (var notificationIndex in notifications) {
                if (notifications.hasOwnProperty(notificationIndex)) {
                    var newNotification = document.createElement('li');
                    newNotification.className = 'hp-notification new-notification';
                    newNotification.innerHTML = notifications[notificationIndex];
                    notificationDropDownLayer.find('li:last').before(newNotification);
                }
            }

            var setOpen = false;
            if (notificationDropDownLayer.hasClass('open')) {
                setOpen = true;
            }

            if (setOpen == true) {
                notificationDropDownLayer.addClass('open');
            }
        } catch (exception) {
            console.log('Client exception: ' + exception);
            console.log('Data from server: ' + event.data);
        }
    }

    // notification WebSocket on error listener
    function wsNotificationOnError(event) {
        console.log('Error: ' + event.data);
    }

    // now init the WebSocket
    wsNotificationInit();
});