/**
 * This class handles all chat related methods.
 *
 * @constructor
 */
function Chat() {
    var chat = this;
    this.maxChars = 2000;
    this.onlineFriends = [];
    this.messages = { user: 'You', textAreaPlaceholder: 'Enter a message...' };

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
                    }
                    found = true;
                    break;
                }
            }

            // if the friend was not found, then he is coming online, add him to the onlineFriends array
            if (!found) {
                this.onlineFriends.push(friends);
            }
        }

        // update the HTML list
        var chatList = $('#hp-chat-list').find('>ul');
        chatList.empty();
        if (this.onlineFriends.length > 0) {
            $('#hp-chat-list-online').removeClass('hidden');
            $('#hp-chat-list-offline').addClass('hidden');
            for (i = 0; i < this.onlineFriends.length; i++) {
                var friend = this.onlineFriends[i];
                var liElement = document.createElement('li');
                liElement.setAttribute('data-friend-id', friend.id);
                liElement.setAttribute('data-friend-name', friend.name);
                liElement.innerHTML = friend.name;
                liElement.addEventListener('click', function(e) {
                    var friendId = e.currentTarget.getAttribute('data-friend-id');
                    var friendName = e.currentTarget.getAttribute('data-friend-name');
                    chat.getChatWindow(friendId, friendName);
                }, false);
                chatList.append(liElement);
            }
        } else {
            $('#hp-chat-list-online').addClass('hidden');
            $('#hp-chat-list-offline').removeClass('hidden');
        }
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
     * @param friendId Account ID of friend
     * @param chatWindow Origin chat window
     */
    this.sendChat = function(friendId, chatWindow) {
        var chatInput = chatWindow.getElementsByTagName('textarea')[0];
        var chatContent = chatWindow.getElementsByClassName('hp-chat-window-content')[0];
        var chatMessageElement = document.createElement('div');
        var textMessage = chatInput.value;

        if (textMessage.length > this.maxChars) {
            textMessage = textMessage.substr(0, this.maxChars);
        }

        var webSocketMessage = {
            'method': 'SendChat',
            'recipient': friendId,
            'text': textMessage
        };

        chatInput.value = '';
        chatMessageElement.className = 'hp-chat-you';
        chatMessageElement.setAttribute('data-name', this.messages.user);
        chatMessageElement.innerHTML = this.htmlMessage(textMessage);
        webSocket.send(webSocketMessage);

        chatContent.appendChild(chatMessageElement);
        chatContent.scrollTop = chatContent.scrollHeight;
    };

    /**
     * Receives a chat message from a friend.
     *
     * @param sender Sender (friend) object
     * @param textMessage Text message from friend
     */
    this.receiveChat = function(sender, textMessage) {
        var chatWindow = this.getChatWindow(sender.id, sender.name);
        var chatContent = chatWindow.getElementsByClassName('hp-chat-window-content')[0];
        var chatMessageElement = document.createElement('div');
        chatMessageElement.className = 'hp-chat-partner';
        chatMessageElement.innerHTML = this.htmlMessage(textMessage);
        chatMessageElement.setAttribute('data-name', sender.name);
        chatContent.appendChild(chatMessageElement);
        chatContent.scrollTop = chatContent.scrollHeight;

        // if current chat window is collapsed let it blink
        if (chatWindow.classList.contains('hp-chat-window-collapsed')) {
            chatWindow.classList.add('hp-chat-window-collapsed-new-message');
        }
    };

    /**
     * Returns a dynamically created chat window.
     * If a specific chat window is not appended to the DOM already, it is
     * created dynamically and appended then. Otherwise it is just returned.
     *
     * @param friendId Account ID of friend
     * @param friendName Name of friend
     * @returns {HTMLElement}
     */
    this.getChatWindow = function(friendId, friendName) {
        // return #hp-chat-window-<friendId> if already appended to DOM
        var foundElement = document.getElementById('hp-chat-window-' + friendId);
        if (foundElement != undefined) {
            return foundElement;
        }

        // create a new chat window with all its elements and attributes
        var chatWindow = document.createElement('div');
        var chatTitle = document.createElement('div');
        var chatContent = document.createElement('div');
        var chatInput = document.createElement('textarea');
        chatWindow.className = 'hp-chat-window hidden-xs hp-chat-window-expanded';
        chatWindow.id = 'hp-chat-window-' + friendId;
        chatTitle.className = 'hp-chat-window-name';
        chatTitle.innerHTML = friendName;
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
                chat.sendChat(friendId, chatWindow);
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

        // append all newly created elements to the main div
        chatWindow.appendChild(chatTitle);
        chatWindow.appendChild(chatContent);
        chatWindow.appendChild(chatInput);

        // Add mouse events to chat window:
        // On mouse over change cursor to pointer if cursor is on name,
        // on click on name toggle chat panel to be visible.
        chatWindow.addEventListener('mouseover', function(e) {
            var currentElement = e.currentTarget;
            var relativeY = e.pageY - currentElement.getBoundingClientRect().top;
            currentElement.style.cursor = (relativeY >= 0 && relativeY <= 28) ? 'pointer' : 'default';
        }, false);
        chatWindow.addEventListener('click', function(e) {
            var currentElement = e.currentTarget;
            var relativeY = e.pageY - currentElement.getBoundingClientRect().top;
            if (relativeY >= 0 && relativeY <= 28) {
                if (currentElement.classList.contains('hp-chat-window-collapsed')) {
                    currentElement.classList.remove('hp-chat-window-collapsed');
                    currentElement.classList.remove('hp-chat-window-collapsed-new-message');
                    currentElement.classList.add('hp-chat-window-expanded');
                } else {
                    currentElement.classList.remove('hp-chat-window-expanded');
                    currentElement.classList.add('hp-chat-window-collapsed');
                }
            }
        }, false);

        // append chat window to bottom "#hp-chats" container
        document.getElementById('hp-chats').appendChild(chatWindow);
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
            currentElement.getElementsByClassName('hp-easy-chat')[0].style.display = 'none';
            document.getElementById('hp-chat-list').classList.remove('hidden');
        } else {
            currentElement.classList.remove('hp-chat-expanded');
            currentElement.classList.add('hp-chat-collapsed');
            currentElement.getElementsByClassName('hp-easy-chat')[0].style.display = 'block';
            document.getElementById('hp-chat-list').classList.add('hidden');
        }
    }, false);
}
