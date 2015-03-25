
function resizeRings() {
	$('.hp-notepad-content').each(function() {
		var offset = ($(this).height() + parseInt($(this).css('padding-top'))) % 12.0;
		if (offset !== 0)
			$(this).css('padding-bottom', (12.0 - offset) + "px");
		else
			$(this).css('padding-bottom', '0');
	});
}

function toggleMediaSelection(parent) {
	var childs = document.getElementById("mediaList").getElementsByTagName("input");
	for (i = 0; i < childs.length; i++) {
		if (!childs[i].disabled)
			childs[i].checked = parent.checked;
	}
}

function autolinkUrls() {
    $('.hp-truncate').each(function(){
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

/*
 *  Options Menu
 */
$('.hp-optionsMenu>div').on('shown.bs.dropdown', function() {
    $(this).find('.dropdown-toggle>span').removeClass('glyphicon-chevron-down').addClass('glyphicon-chevron-up');
    var menu = $(this).find('ul.dropdown-menu');
    var row = $(this).parents('tr');
    var top = row.offset().top + row.height() - $('.hp-notepad-right').offset().top;
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

        var old_content = replaceContentWithLoadingIndicator(post_container);
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
                post_container.find(".commentSubmit").click(function () {
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
                            post_container.html(old_content); // put back removed content
                        }
                    });

                    replaceContentWithLoadingIndicator(post_container);
                    return false;
                });
            }
        });
        return false;
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


$(document).ready(function () {

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
			autolinkUrls();
		}
	});

	/*
	 * ADD COMMENTS
	 */
	$('.hp-comment-form').each(function(){
		var context = $(this);
		$(".commentSubmit", this).click(function(){
			if(context.serializeArray()[0].value === ""){
				$(context).find('textarea').animate({opacity:0.3},100,"linear",function(){
					$(this).animate({opacity:1},100);
					$(this).focus();
				});
			} else {
				$.ajax({
					url: context.attr('action'),
					type: "POST",
					data: context.serialize(),
					success: function(data){
						context.before(data);
						context[0].reset();
					}, error: function() {
                        $(context).find('textarea').animate({opacity:0.3},100,"linear",function(){
                            $(this).animate({opacity:1},100);
                            $(this).focus();
                        });
                    }
				});
			}
			return false;
		});
	});

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
			window.setTimeout(resizeRings(), 400);
			return false;
		});
	});

    autolinkUrls();

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
                    if(item._type === 'user') {
                        label = item._source.name;
                        hLabel = item.highlight.name;
                    }
                    if(item._type === 'group') {
                        label = item._source.title;
                        hLabel = item.highlight.title;
                        groupType = item._source.grouptype;
                        if(groupType === 'open') groupIcon = 'globe';
                        if(groupType === 'close') groupIcon = 'lock';
                        if(groupType === 'course') groupIcon = 'briefcase';
                    }
                    result.push({
                        label: label,
                        hLabel: hLabel,
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

    $('.hp-easy-search').typeahead(
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
                    "{{#if avatar}} " +
                    "<img class='autosuggest-user-avatar' src='/assets/images/avatars/{{avatar}}.png' alt='picture'>{{{hLabel}}}" +
                    "{{/if}}" +
                    "{{#if groupIcon}}" +
                    "<span class='glyphicon glyphicon-{{groupIcon}} autosuggest-group-icon'></span>{{{hLabel}}}" +
                    "{{/if}}")
            }

        }).on('typeahead:selected', function($e, searchResult){
            window.location.href = window.location.origin + "/"+searchResult.type+"/" + searchResult.id + '/stream';
        });
});

$(window).resize(function() {
	resizeRings();
	truncateBreadcrumb();
});

$(window).load(function() {
});

$('body').tooltip({
    selector: '[rel=tooltip]'
});
$('body').popover({
    trigger: 'hover',
    selector: '[rel="popover"]'
});

$('.hp-focus-search').click(function() {
    $('.hp-easy-search').focus();
});

resizeRings();
truncateBreadcrumb();
