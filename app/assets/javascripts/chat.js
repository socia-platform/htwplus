/**
 * This class handles all chat related methods.
 *
 * @constructor
 */
/* jshint ignore:start */
function Chat() {
    var chat = this;
    this.maxChars = 2000;
    this.onlineFriends = [];
    this.messages = {
        user: 'You',
        textAreaPlaceholder: 'Enter a message...',
        textAreaPlaceholderOffline: 'Unfortunately your friend is currently offline...',
        online: 'online',
        offline: 'offline'
    };

    /**
     * Updates the friends list.
     *
     * @param friends Account object or list of account objects
     * @param status Status of the friend(s)
     */
    this.updateFriendList = function(friends, status) {
        var i;

        // check, if we have a list of friends or a single friend
        if (Object.prototype.toString.call(friends) === '[object Array]') {
            this.onlineFriends = friends;
        } else {
            var found = false;

            // we have a single friend, let's try to find him in our onlineFriends array
            for (i = 0; i < this.onlineFriends.length; i++) {
                if (this.onlineFriends[i].id == friends.id) {
                    // We found the specific friend in our friend online array, either he's already online
                    // then we can ignore, or he is going offline, then we have to remove him from the array.
                    if (status == "FriendOffline") {
                        this.onlineFriends.splice(i, 1);
                        this.setStatusChatWindow(friends.id, friends.name, this.messages.offline);
                    }

                    found = true;
                    break;
                }
            }

            // if the friend was not found, then he is coming online, add him to the onlineFriends array
            if (!found) {
                this.onlineFriends.push(friends);
                this.setStatusChatWindow(friends.id, friends.name, this.messages.online);
            }
        }

        // update the HTML list by removing all children from HTML list
        // and append all new list elements
        var chatList = document.getElementById('hp-chat-list').getElementsByTagName('ul')[0];
        var chatListOnline = document.getElementById('hp-chat-list-online');
        var chatListOffline = document.getElementById('hp-chat-list-offline');
        while (chatList.hasChildNodes()) {
            chatList.removeChild(chatList.lastChild);
        }

        if (this.onlineFriends.length > 0) {
            chatListOnline.classList.remove('hidden');
            chatListOffline.classList.add('hidden');
            for (i = 0; i < this.onlineFriends.length; i++) {
                var friend = this.onlineFriends[i];
                var liElement = document.createElement('li');
                liElement.setAttribute('data-friend-id', friend.id);
                liElement.setAttribute('data-friend-name', friend.name);
                liElement.innerHTML = friend.name;
                liElement.addEventListener('click', function(e) {
                    var friend = {
                        id: e.currentTarget.getAttribute('data-friend-id'),
                        name: e.currentTarget.getAttribute('data-friend-name')
                    };
                    chat.getChatWindow(friend);
                }, false);
                chatList.appendChild(liElement);
            }
        } else {
            chatListOnline.classList.add('hidden');
            chatListOffline.classList.remove('hidden');
        }

        document.getElementById('hp-chat-list-collapsed-counter').innerHTML = '(' + this.onlineFriends.length.toString() + ')';
    };

    /**
     * Returns a HTML converted text message.
     *
     * @param message Chat message
     * @returns {string}
     */
    this.htmlMessage = function(message) {
        return message
            .replace(/  /g, '&nbsp;&nbsp;')
            .replace(/\n/g, '<br>')
            .replace(/\r/g, '<br>')
            .replace(/\t/g, '&nbsp;&nbsp;&nbsp;&nbsp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    };

    /**
     * Sends a chat message to a friend.
     *
     * @param friend Account of friend
     * @param chatWindow Origin chat window
     */
    this.sendChat = function(friend, chatWindow) {
        var chatInput = chatWindow.getElementsByTagName('textarea')[0];
        var textMessage = chatInput.value;
        if (textMessage.length > this.maxChars) {
            textMessage = textMessage.substr(0, this.maxChars);
        }
        chatInput.value = '';

        // send WebSocket message
        webSocket.send({'method': 'SendChat', 'recipient': friend.id, 'text': textMessage});

        var htmlMessage = this.htmlMessage(textMessage);
        this.addToChatStorage(friend, null, htmlMessage);
        this.appendChatContent(chatWindow, null, htmlMessage);
    };

    /**
     * Receives a chat message from a friend.
     *
     * @param sender Sender (friend) object
     * @param textMessage Text message from friend
     */
    this.receiveChat = function(sender, textMessage) {
        var chatWindow = this.getChatWindow(sender);
        var htmlMessage = this.htmlMessage(textMessage);
        this.addToChatStorage(sender, sender, htmlMessage);
        this.appendChatContent(chatWindow, sender, htmlMessage);

        var chatWindowStatus = 'expanded';
        if (chatWindow.classList.contains('hp-chat-window-collapsed-new-message')) {
            chatWindowStatus = chatWindow.classList.contains('hp-chat-window-collapsed-new-message')
                ? 'collapsed-new-message' : 'collapsed';
        }
        this.setChatStorageWindowStatus(sender, chatWindowStatus);
    };

    /**
     * Appends a new chat message to a chat window.
     *
     * @param chatWindow Chat window
     * @param sender Account ID of friend if receiving message or null, if sending message
     * @param htmlMessage HTML converted message
     */
    this.appendChatContent = function(chatWindow, sender, htmlMessage) {
        var chatContent = chatWindow.getElementsByClassName('hp-chat-window-content')[0];
        var chatMessageElement = document.createElement('div');
        chatMessageElement.className = sender == null ? 'hp-chat-you' : 'hp-chat-partner';
        chatMessageElement.innerHTML = htmlMessage;
        chatMessageElement.setAttribute('data-name', sender ? sender.name : this.messages.user);
        chatContent.appendChild(chatMessageElement);
        chatContent.scrollTop = chatContent.scrollHeight;

        // notify the user (if chat window is collapsed
        this.blinkChatWindow(chatWindow, 3);
    };

    /**
     * Blinks a collapsed chat window for count times.
     *
     * @param chatWindow The chat window to blink
     * @param count Blink count
     */
    this.blinkChatWindow = function(chatWindow, count) {
        // if chat window is not collapsed, abort
        if (!chatWindow.classList.contains('hp-chat-window-collapsed')) {
            return;
        }

        if (chatWindow.classList.contains('hp-chat-window-collapsed-new-message')) {
            chatWindow.classList.remove('hp-chat-window-collapsed-new-message');
        } else {
            chatWindow.classList.add('hp-chat-window-collapsed-new-message');
            count--;
        }

        // blink again, if blinking times are not run out
        if (count > 0) {
            window.setTimeout(function() { chat.blinkChatWindow(chatWindow, count); }, 750);
        }
    };

    /**
     * Returns chat storage for a friend or an empty list, if not available yet.
     * If session storage is not supported, it returns null.
     *
     * @param friend Account of friend
     * @returns {*}
     */
    this.getChatStorage = function(friend) {
        // if we have no session storage support, return null
        if (typeof(Storage) == 'undefined') {
            return null;
        }

        var chatStorage = sessionStorage.getItem(friend.id.toString());
        if (chatStorage == null) {
            chatStorage = { windowStatus: 'expanded', friend: friend, messages: [] };
        } else {
            chatStorage = JSON.parse(chatStorage);
        }

        return chatStorage;
    };

    /**
     * Sets the window status for a chat storage.
     *
     * @param friend Account of friend
     * @param status Status of chat window
     */
    this.setChatStorageWindowStatus = function(friend, status) {
        var chatStorage = this.getChatStorage(friend);

        // if null is returned, session storage is not available, abort
        if (chatStorage == null) {
            return;
        }

        chatStorage.windowStatus = status;
        sessionStorage.setItem(friend.id.toString(), JSON.stringify(chatStorage));
    };

    /**
     * Adds a chat message to the session storage.
     *
     * @param friend Account of friend
     * @param sender Account of sender (friendId or null for own user)
     * @param message Text message
     */
    this.addToChatStorage = function(friend, sender, message) {
        var chatStorage = this.getChatStorage(friend);

        // if null is returned, session storage is not available, abort
        if (chatStorage == null) {
            return;
        }

        // if our array has more than 10 elements, remove the first one
        var chatMessages = chatStorage.messages;
        if (chatMessages.length >= 10) {
            chatMessages = chatMessages.splice(1, chatMessages.length - 1);
        }
        chatMessages.push({s: sender ? sender.id : null, m: message});
        chatStorage.messages = chatMessages;

        sessionStorage.setItem(friend.id.toString(), JSON.stringify(chatStorage));
    };

    /**
     * Loads previous chats if available on session storage.
     */
    this.loadPreviousChats = function () {
        // if we have no session storage support, return null
        if (typeof(Storage) == 'undefined') {
            return;
        }

        // load all chat storage elements
        for (var i = 0; i < sessionStorage.length; i++) {
            var chatStorage = this.getChatStorage({ id: sessionStorage.key(i) });
            if (chatStorage.windowStatus != 'closed') {
                this.getChatWindow(chatStorage.friend);
            }
        }
    };

    /**
     * Sets a status of a friend, if a chat window is already available.
     *
     * @param friendId Account ID of friend
     * @param friendName Name of friend
     * @param status Status of friend
     */
    this.setStatusChatWindow = function(friendId, friendName, status) {
        var chatWindow = document.getElementById('hp-chat-window-' + friendId);
        if (chatWindow == undefined) {
            return;
        }

        var chatWindowTitle = chatWindow.getElementsByClassName('hp-chat-window-title-name')[0];
        var chatWindowInput = chatWindow.getElementsByTagName('textarea')[0];
        chatWindowTitle.innerHTML = status == this.messages.offline ? friendName + ' (' + status + ')' : friendName;
        chatWindowInput.readOnly = status == this.messages.offline;
        chatWindowInput.setAttribute('placeholder',
            status == this.messages.offline ? this.messages.textAreaPlaceholderOffline : this.messages.textAreaPlaceholder
        );
    };

    /**
     * Returns a dynamically created chat window.
     * If a specific chat window is not appended to the DOM already, it is
     * created dynamically and appended then. Otherwise it is just returned.
     *
     * @param friend Account of friend
     * @returns {HTMLElement}
     */
    this.getChatWindow = function(friend) {
        // return #hp-chat-window-<friendId> if already appended to DOM
        var chatWindow = document.getElementById('hp-chat-window-' + friend.id);
        if (chatWindow != undefined) {
            return chatWindow;
        }

        // create a new chat window with all its elements and attributes
        chatWindow = document.createElement('div');
        var chatTitle = document.createElement('div');
        var chatContent = document.createElement('div');
        var chatInput = document.createElement('textarea');
        var chatTitleName = document.createElement('div');
        var chatTitleMinimize = document.createElement('div');
        var chatTitleClose = document.createElement('div');
        chatWindow.className = 'hp-chat-window hidden-xs hp-chat-window-expanded';
        chatWindow.id = 'hp-chat-window-' + friend.id;

        // title
        chatTitle.className = 'hp-chat-window-title';
        chatTitleName.className = 'hp-chat-window-title-name';
        chatTitleName.innerHTML = friend.name;
        chatTitleMinimize.className = 'hp-chat-window-title-button hp-chat-window-title-button-minimize';
        chatTitleClose.className = 'hp-chat-window-title-button hp-chat-window-title-button-close';

        // content and text area
        chatContent.className = 'hp-chat-window-content well';
        chatInput.className = 'form-control';
        chatInput.setAttribute('maxlength', this.maxChars.toString());
        chatInput.setAttribute('placeholder', this.messages.textAreaPlaceholder);

        // Add key events to the textarea. Send chat with enter (keyCode == 13).
        // To input a break, the user can type shift-enter. For this, we need to
        // know, if shift (keyCode == 16) is pressed while hitting enter.
        var isShiftPressed = false;
        chatInput.addEventListener('keydown', function(e) {
            if (e.keyCode == 13 && !isShiftPressed) {
                chat.sendChat(friend, chatWindow);
                e.preventDefault();
            } else if (e.keyCode == 16) {
                isShiftPressed = true;
            }
        }, false);
        chatInput.addEventListener('keyup', function(e) {
            if (e.keyCode == 16) {
                isShiftPressed = false;
            }
        }, false);

        // append all title elements to the title div
        chatTitle.appendChild(chatTitleName);
        chatTitle.appendChild(chatTitleMinimize);
        chatTitle.appendChild(chatTitleClose);

        // append all newly created elements to the main div
        chatWindow.appendChild(chatTitle);
        chatWindow.appendChild(chatContent);
        chatWindow.appendChild(chatInput);

        // add mouse events to chat window title buttons
        chatTitleMinimize.addEventListener('click', function(e) {
            var chatWindow = e.currentTarget.parentNode.parentNode;
            if (chatWindow.classList.contains('hp-chat-window-collapsed') || chatWindow.classList.contains('hp-chat-window-collapsed-new-message')) {
                chatWindow.classList.remove('hp-chat-window-collapsed');
                chatWindow.classList.remove('hp-chat-window-collapsed-new-message');
                chatWindow.classList.add('hp-chat-window-expanded');
                chat.setChatStorageWindowStatus(friend, 'expanded');
            } else {
                chatWindow.classList.remove('hp-chat-window-expanded');
                chatWindow.classList.add('hp-chat-window-collapsed');
                chat.setChatStorageWindowStatus(friend, 'collapsed');
            }
        }, false);
        chatTitleClose.addEventListener('click', function(e) {
            var chatWindow = e.currentTarget.parentNode.parentNode;
            chatWindow.parentNode.removeChild(chatWindow);
            chat.setChatStorageWindowStatus(friend, 'closed');
        });

        // append chat window to bottom "#hp-chats" container before first node (if exists)
        var chats = document.getElementById('hp-chats');
        if (chats.hasChildNodes()) {
            chats.insertBefore(chatWindow, chats.childNodes[0]);
        } else {
            chats.appendChild(chatWindow);
        }

        // append chats from session storage, if available
        var chatStorage = this.getChatStorage(friend);
        if (chatStorage != null) {
            for (var i = 0; i < chatStorage.messages.length; i++) {
                var message = chatStorage.messages[i];
                this.appendChatContent(chatWindow, message.s ? chatStorage.friend : null, message.m);
            }
            chatWindow.classList.remove('hp-chat-window-expanded');
            chatWindow.classList.add('hp-chat-window-' + chatStorage.windowStatus);
        }

        // set focus to textarea input
        chatWindow.getElementsByTagName('textarea')[0].focus();

        return chatWindow;
    };

    /**
     * Toggle friends list chat panel
     */
    var friendList = document.getElementById('hp-chat');
    friendList.addEventListener('click', function(e) {
        var currentElement = e.currentTarget;
        if (currentElement.classList.contains('hp-chat-collapsed')) {
            currentElement.classList.remove('hp-chat-collapsed');
            currentElement.classList.add('hp-chat-expanded');
            document.getElementById('hp-chat-list-collapsed').style.display = 'none';
            document.getElementById('hp-chat-list').classList.remove('hidden');
        } else {
            currentElement.classList.remove('hp-chat-expanded');
            currentElement.classList.add('hp-chat-collapsed');
            document.getElementById('hp-chat-list-collapsed').style.display = 'block';
            document.getElementById('hp-chat-list').classList.add('hidden');
        }
    }, false);
}
/* jshint ignore:end */