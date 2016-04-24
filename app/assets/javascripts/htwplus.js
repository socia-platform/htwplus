(function($){
  $.fn.markdown.messages.de = {
    'Bold': "Fett",
    'Italic': "Kursiv",
    'Heading': "Überschrift",
    'URL/Link': "Ein Link einfügen",
    'Image': "Ein Bild einfügen",
    'List': "Aufzählungszeichen",
    'Ordered List': "Nummerierung",
    'Unordered List': "Aufzählungszeichen",
    'Code': "Quellcode",
    'code text here': "",
    'Quote': "Zitat",
    'quote here': "",
    'Preview': "Vorschau",
    'strong text': "",
    'emphasized text': "",
    'heading text': "",
    'enter link description here': "Linkbeschreibung",
    'Insert Hyperlink': "Link zum Webseite",
    'enter image description here': "Bildbeschreibung",
    'Insert Image Hyperlink': "Link zum Bild",
    'enter image title here': "Bildtitel",
    'list text here': "",
    'Save': "Posten"
  };
}(jQuery));

Dropzone.autoDiscover = false;

function toggleMediaSelection(parent) {
	var childs = document.getElementById("mediaList").getElementsByTagName("input");
	for (i = 0; i < childs.length; i++) {
		if (!childs[i].disabled)
			childs[i].checked = parent.checked;
	}
}

function autolinkUrls() {
    $('.hp-post').each(function(){
		$(this).linkify({
			tagName: 'a',
			target: '_blank',
			newLine: '\n',
			linkClass: 'hp-postLink',
			linkAttributes: null
		});
	});
	$('.hp-postLink').each(function(){
        if (!$(this).find("span").length)
			$(this).append(" <span class='glyphicon glyphicon-share-alt'></span>");
	});
}

function linkOlderComments() {
    /*
	 * SHOW OLDER COMMENTS
	 */
	$('.olderComments').each(function(){
		var id = $(this).attr('href').split('-')[1];
		var context = this;
		$(this).click(function(){
			if($(context).hasClass('open')){
				$("#collapse-"+id).collapse('toggle');
				$(context).html("Ältere Kommentare anzeigen...");
				$(context).removeClass('open');
				$(context).addClass('closed');
			}
			else if($(context).hasClass('closed')){
				$("#collapse-"+id).collapse('toggle');
				$(context).html("Ältere Kommentare ausblenden...");
				$(context).removeClass('closed');
				$(context).addClass('open');
			}
			else if($(context).hasClass('unloaded')){
				var currentComments = $('#comments-' + id + ' > .media').length;
				$(context).html("Ältere Kommentare ausblenden...");
				$.ajax({
					url: "/post/olderComments?id=" + id + "&current=" + currentComments,
					type: "GET",
					success: function(data){
						$("#collapse-"+id).html(data);
						$("#collapse-"+id).collapse('toggle');
					}
				});
				$(context).addClass('open');
				$(context).removeClass('unloaded');
			}

			return false;
		});
	});
}

function linkAddComments() {
	/*
	 * ADD COMMENTS
	 */
    $('.hp-comment-form').each(function () {
        var context = $(this);
        // avoid register click event multiple times (pagination)
        $(".commentSubmit", this).off().on('click', function () {
            if (context.serializeArray()[0].value.trim() === "") {
                $(context).find('textarea').animate({opacity: 0.3}, 100, "linear", function () {
                    $(this).animate({opacity: 1}, 100);
                    $(this).focus();
                }).focus();
            } else {
                $.ajax({
                    url: context.attr('action'),
                    type: "POST",
                    data: context.serialize(),
                    success: function (data) {
                        context.before(data);
                        context[0].reset();
                    }, error: function () {
                        $(context).find('textarea').animate({opacity: 0.3}, 100, "linear", function () {
                            $(this).animate({opacity: 1}, 100);
                            $(this).focus();
                        });
                    }
                });
            }
            return false;
        });
    });
}

function truncateBreadcrumb() {
	var lastBreadcrumb = $("#hp-navbar-breadcrumb .breadcrumb > li:last-child");
	var index = 3;	// first breadcrumb item which is hidden
	// hide breadcrumb items while last item isn't visible
	while (lastBreadcrumb.length &&
		lastBreadcrumb.position().left + lastBreadcrumb.width() > $("#hp-navbar-breadcrumb .breadcrumb").width()) {
		$("#hp-navbar-breadcrumb #hp-navbar-breadcrumb-truncate").removeClass("hidden");
		$("#hp-navbar-breadcrumb .breadcrumb > li:nth-child("+index+")").addClass("hidden");
		index++;
	}
}

function showAllBreadcrumbItems() {
	$("#hp-navbar-breadcrumb .breadcrumb > li").removeClass("hidden");
	$("#hp-navbar-breadcrumb #hp-navbar-breadcrumb-truncate").addClass("hidden");
}

/** replaces the content of the given element with a loading indicator, and returns the old content (as html) **/
function replaceContentWithLoadingIndicator(element) {
    var old_content = element.html();
    element.html("<div class=\"loading\"></div>");
    element.find(".loading").show();
    return old_content;
}

/** show an error before/above the given element **/
function showErrorBeforeElement(element, error_message) {
    element.before('<div class="alert alert-danger"><a data-dismiss="alert" class="close" href="#">×</a>'+error_message+'</div>');
}

/** markdown post content */
function markdownPostContent() {
    $('.hp-post').each(function( index, value ) {
        // check if element is already marked (pagination issue)
        if (!$(this).hasClass('marked')) {
            this.innerHTML = md.render(value.textContent);
            $(this).addClass('marked');
        }
    });
}

/*
 *  Options Menu
 */
$('.hp-optionsMenu>div').on('shown.bs.dropdown', function() {
    $(this).find('.dropdown-toggle>span').removeClass('glyphicon-chevron-down').addClass('glyphicon-chevron-up');
    var menu = $(this).find('ul.dropdown-menu');
    var row = $(this).parents('tr');
    // hacky: 45 belongs to div.hp-notepad-content.addmargin    
    var top = row.offset().top - $('.hp-notepad-content').offset().top;
    menu.css('top', top + 'px');
});

$('.hp-optionsMenu>div').on('hidden.bs.dropdown', function() {
    $(this).find('.dropdown-toggle>span').removeClass('glyphicon-chevron-up').addClass('glyphicon-chevron-down');
});

$(".hp-optionsTable>tr").bind("contextmenu", function (e) {
    e.preventDefault();
    $(this).find('.hp-optionsMenu .dropdown-toggle').trigger("click");
});

$(".hp-optionsTable>tr>td:not(.hp-optionsMenu)").on("click", function(e) {
    e.preventDefault();
    $(this).parent().trigger('contextmenu');
    return false;
});

$(".hp-optionsTable>tr>td>a").on("click", function(e) {
    // links in tables
    e.stopPropagation();
});

$(".hp-optionsTable>tr>td>input").on("click", function(e) {
    // checkbox in media list
    e.stopPropagation();
});

/*
 * EDIT COMMENTS
 */
$('body').on('click', 'a.hp-post-edit', function(e) {
    if($(e.currentTarget).hasClass("disabled"))
        return false;
    else {
        var post_id = e.currentTarget.id.split("_")[1];
        var post_container = $("#"+post_id);

        //var old_content = replaceContentWithLoadingIndicator(post_container);
        post_container.removeClass('marked');
        var removed_classes = post_container.attr("class");
        post_container.attr("class", ""); // remove the classes (preventing linkify and whitespace stuff to apply)
        post_container.load("/post/"+post_id+"/getEditForm", function(response, status, xhr) {
            if (status == "error") {
                console.log("Error when trying to edit post: ["+status+"]");
                showErrorBeforeElement(post_container, '<strong>Ein Fehler ist aufgetreten!</strong> <a class="hp-reload" href="#">Bitte laden Sie die Seite neu!</a> (Vielleicht ist der Bearbeitungszeitraum zuende?)');
                $(".hp-reload").click(function() {
                    window.location.reload();
                });
                post_container.html(old_content); // put back removed content
            } else {
                $("#hp-edit-post-content").markdown({
                    savable: true,
                    language: 'de',
                    autofocus: true,
                    onShow: function(e) {
                        $(".hp-post-content button[data-handler='cmdSave']").html('<span class="glyphicon glyphicon-refresh"></span> Aktualisieren');
                    },
                    onPreview: function(e) {
                        return md.render(e.getContent());
                    },
                    onSave: function(e) {
                        var form = post_container.find("form");
                        $.ajax({
                            url: form.attr('action'),
                            type: "POST",
                            data: form.serialize(),
                            success: function (data) {
                                post_container.html(data);
                                post_container.attr("class", removed_classes);
                            },
                            error: function(xhr, status, errorThrown) {
                                console.log("Error when submitting edited post: ["+status+"] " + errorThrown);
                                showErrorBeforeElement(post_container, '<strong>Ein Fehler ist aufgetreten!</strong> <a class="hp-reload" href="#">Bitte laden Sie die Seite neu!</a> (Vielleicht ist der Bearbeitungszeitraum zuende?)');
                                $(".hp-reload").click(function() {
                                    window.location.reload();
                                });
                            }
                        });
                    },
                    dropZoneOptions: {
                        url: "/media/upload/"+folderToUpload,
                        clickable: '.hp-dropzone-edit-clickable',
                        previewsContainer: '.hp-dropzone-edit-preview'
                    }
                });
            }
        });
        return false;
    }
});

/*
 * prevent submitting empty posts
 */
$(".hp-post-form").on("submit", function(e) {
    if($(this).find("textarea").val().trim().length <= 0) {
        e.preventDefault();
        $(this).find("textarea").animate({opacity:0.1},100,"linear",function() { // blink and focus textarea
            $(this).animate({opacity:1},100);
            $(this).focus();
        }).focus();
    }
});

/*
 *  prevent click action for disabled list items
 */
$("li > a").click(function(e) {
	if ($(this).parent().hasClass('disabled')) {
		e.preventDefault();
		return false;
	}
});

/*
 *  prevent easy copying of account deletion confirmation text
 */
$(document).on("copy", function(e) {
    if ($("#hp-deleteModal").is(":visible")) { // if the deletion confirmation is actually visible
        var selection = window.getSelection();
        if (selection.toString().contains("ösche ich meinen Account von dieser wundervolle")) { // check if the user copied the 'forbidden' string (or at least the middle part of it)
            var newdiv = document.createElement('div');

            //hide the newly created container
            newdiv.style.position = 'absolute';
            newdiv.style.left = '-9999px';

            //insert the container, fill it with the extended text, and define the new selection
            document.body.appendChild(newdiv);
            newdiv.innerHTML = "It's not that easy!";
            selection.selectAllChildren(newdiv);

            window.setTimeout(function () {
                document.body.removeChild(newdiv);
            }, 100);
        }
    }
});

/*
 *  show "create folder" form (group media) and focus input field
 */
$('body').on('click', 'a.hp-create-folder', function(e) {
    e.preventDefault();
    $("#hp-create-folder-wrapper").removeClass('hidden');
    $("#hp-create-folder-wrapper").find('input').focus();
});

/*
 * submit media and folder deletion form
 */
$('body').on('click', '.hp-mediaList-submit', function (e) {
    $('#mediaListFrom').append('<input type="hidden" name="action" value="delete">').submit();
});

/*
 * Markdown definition
 */
var md = window.markdownit({
               html: false,
               breaks: true,
               linkify: false,
               typographer: true
             }).use(window.markdownitMark).use(window.markdownitEmoji);

md.renderer.rules.emoji = function(token, idx) {
  return '<img class="emoji" width="20" height="20" src="' + location.origin + '/assets/images/emojis/' + token[idx].markup + '.png" />';
};

// find folder to upload to
var folderToUpload = 0;
if ($('#folderToUpload').size() > 0) {
    folderToUpload = $('#folderToUpload')[0].innerText;
}

// apply markdown editor
$("#hp-new-post-content").markdown({
    savable: true,
    language: 'de',
    onPreview: function(e) {
        return md.render(e.getContent());
    },
    onSave: function(e) {
        $('#hp-post-submit-button').click();
    },
    dropZoneOptions: {
        url: "/media/upload/"+folderToUpload,
        clickable: '.hp-dropzone-clickable',
        previewsContainer: '.hp-dropzone-preview',
        parallelUploads: 1
    }
});

$(document).ready(function () {

    markdownPostContent();

    /*
     *  Token
     */
	var preSelection = $("input:radio[name=type]:checked").val();
	if(preSelection == 2) {
		$("#token-input").show();
	}
	$("input:radio[name=type]").click(function() {
		var selection = $(this).val();
		if(selection == 2) {
			$("#token-input").fadeIn();
		} else {
			$("#token-input").fadeOut();
		}

	});

	/*
	 * AJAX loading indicator
	 */
	$.ajaxSetup({
		beforeSend:function(){
			$(".loading").show();
			$(".loading").css('display', 'inline-block');
		},
		complete:function(){
			$(".loading").hide();
			markdownPostContent();
			autolinkUrls();
			linkOlderComments();
			linkAddComments();
		}
	});

    /*
     * Auto-pagination with jQuery plugin (modified version of jquery.auto.pagination.js)
     */
    if($('a.nextPage').length > 0) { // only apply on pages with a nextPage link
        $('.hp-pagination-container').AutoPagination({
            nextPageSelector: 'a.nextPage',
            panelSelector: '.hp-pagination-element',
            loaderDivClass: 'ajax-loader',
            loaderDivStyle: 'text-align:center;margin-top:20px;font-weight:bold;',
            loaderImage: '/assets/images/loading.gif',
            loaderText: 'Lade nächste Seite...'
        });
    }

    /*
     * Show 'Back to top'-link (src: http://jsfiddle.net/panman8201/mkzrm/10/)
     */
    if (($(window).height() + 200) < $(document).height() ) {
        $('#hp-top-link').removeClass('hidden').affix({
            // how far to scroll down before link "slides" into view
            offset: { top:200 }
        });
        $('#hp-top-link a').click(function(event) {
            $('html,body').animate({scrollTop:0});
            return false;
        });
    }

    linkAddComments();

	linkOlderComments();

    autolinkUrls();

    /*
     * Add Countdown to Account deletion button
     */
    $("#hp-deleteModal").on("show.bs.modal", function() {
        $("#hp-deleteConfirmSubmit").attr("disabled", "disabled");

        if($.disableDeleteFunctionTimeout) {
            clearTimeout($.disableDeleteFunctionTimeout);
        }

        var disableTimeLeft = 10;
        var disableCountdown = function() {
            if(disableTimeLeft > 0) {
                $("#hp-deleteConfirmSubmit").val("Warte "+disableTimeLeft+"s...");
                disableTimeLeft--;
                $.disableDeleteFunctionTimeout = setTimeout(disableCountdown, 1000);
            } else {
                $("#hp-deleteConfirmSubmit").removeAttr("disabled");
                $("#hp-deleteConfirmSubmit").val("LÖSCHEN");
            }
        };
        disableCountdown();
    });

    /*
     * SEARCH: AutoSuggestion
     */
    var autoSuggestResult = new Bloodhound({
        datumTokenizer: Bloodhound.tokenizers.obj.whitespace('value'),
        queryTokenizer: Bloodhound.tokenizers.whitespace,
        limit: 10,
        remote: {
            url: '/suggestions?query=',
            rateLimitWait: 0,
            replace: function(url, uriEncodedQuery) {
                return url + uriEncodedQuery.toLowerCase();
            },
            filter: function(parsedResponse) {
                var result = [];
                $.map(parsedResponse.hits.hits, function(item) {
                    var label = '';
                    var hLabel = '';
                    var groupType = '';
                    var groupIcon = '';
                    var custom_avatar = false;
                    if(item._type === 'user') {
                        label = item._source.name;
                        hLabel = item.highlight.name[0].replace('[startStrong]', '<strong>').replace('[endStrong]', '</strong>');
                        if(item._source.avatar === 'custom') {custom_avatar = true;}
                    }
                    if(item._type === 'group') {
                        label = item._source.title;
                        hLabel = item.highlight.title[0].replace('[startStrong]', '<strong>').replace('[endStrong]', '</strong>');
                        groupType = item._source.grouptype;
                        if(item._source.avatar) {
                            custom_avatar = true;
                        } else {
                            if(groupType === 'open') groupIcon = 'globe';
                            if(groupType === 'close') groupIcon = 'lock';
                            if(groupType === 'course') groupIcon = 'briefcase';
                        }
                    }
                    result.push({
                        label: label,
                        hLabel: hLabel,
                        initial: item._source.initial,
                        custom_avatar: custom_avatar,
                        id: item._id,
                        type: item._type,
                        avatar: item._source.avatar,
                        groupType: groupType,
                        groupIcon: groupIcon
                    });
                });
                return result;
            }
        }
    });

    autoSuggestResult.initialize();

    $('.hp-search-form .form-control').typeahead(
        {
            hint: true,
            highlight: false,
            minLength: 2
        },
        {
            name: 'accounts-and-groups',
            displayKey: 'label',
            source: autoSuggestResult.ttAdapter(),

            templates: {
                empty: [
                    '<div class="autosuggest-empty-message">',
                    'Kein Gruppe oder Person gefunden.',
                    '</div>'
                ].join('\n'),
                suggestion: Handlebars.compile("" +
                    "{{#if custom_avatar}} " +
                    "<img class='autosuggest-custom-avatar hp-avatar-small' src='/{{type}}/{{id}}/avatar' alt='avatar'>{{{hLabel}}}" +
                    "{{else}}" +
                    "{{#if avatar}}" +
                    "<div class='autosuggest-avatar hp-avatar-small hp-avatar-default-{{avatar}}'>{{initial}}</div>" +
                    "<div class='autosuggest-username'>{{{hLabel}}}</div>" +
                    "{{/if}}" +
                    "{{/if}}" +
                    "{{#if groupIcon}}" +
                    "<span class='glyphicon glyphicon-{{groupIcon}} autosuggest-group-icon'></span>{{{hLabel}}}" +
                    "{{/if}}")
            }
        }
    ).on('typeahead:selected', function($e, searchResult){
        window.location.href = window.location.origin + "/"+searchResult.type+"/" + searchResult.id + '/stream';
    });
});

$(window).resize(function() {
	truncateBreadcrumb();
});

$('body').tooltip({
    selector: '[rel=tooltip]'
});
$('body').popover({
    trigger: 'hover',
    selector: '[rel="popover"]'
});

/*
 * SET OR REMOVE BOOKMARKS
 */
$('.hp-pagination-container').on('click', 'a.hp-post-bookmark-icon', function() {
    var id = $(this).attr('href').split('-')[1];
    var context = this;
    var icon = this.children[0];
    $.ajax({
     url: "/post/"+id + "/bookmark",
     type: "PUT",
     success: function(data){
         if(data === "setBookmark") {
             $(icon).addClass('glyphicon-floppy-saved');
             $(icon).removeClass('glyphicon-floppy-disk');
             $(context).attr("data-original-title", "Post vergessen").tooltip('fixTitle').tooltip('show');
         }
         if(data === "removeBookmark") {
             $(icon).addClass('glyphicon-floppy-disk');
             $(icon).removeClass('glyphicon-floppy-saved');
             $(context).attr("data-original-title", "Post merken").tooltip('fixTitle').tooltip('show');
         }
     }
    });
});

truncateBreadcrumb();

/*
 * EXPAND PROFILE/GROUP TEXT
 */
$('#hp-profile-header .bottomline .text').readmore({
    collapsedHeight: 47,
    moreLink: '<a href="#">... mehr</a>',
    lessLink: '<a href="#">schließen</a>'
});

$('#hp-profile-header .bottomline .hp-avatar-wrapper').readmore({
    collapsedHeight: 43,
    moreLink: '<a href="#">... weitere</a>',
    lessLink: '<a href="#">schließen</a>'
});

/*
 * ENABLE DROPZONE FOR GROUP UPLOAD
 */
$("form#groupUploadDropzone").dropzone({
    init: function() {
        this.on("queuecomplete", function() {

        });
    },
    parallelUploads: 1,
    dictDefaultMessage: '<span class="glyphicon glyphicon-upload"></span> Datei(en) durch Klick oder Drag&Drop auswählen',
});