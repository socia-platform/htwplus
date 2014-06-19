function getCurrentStyle (element, cssPropertyName) {
   if (window.getComputedStyle) {
     return window.getComputedStyle(element, '').getPropertyValue(cssPropertyName.replace(/([A-Z])/g, "-$1").toLowerCase());
   }
   else if (element.currentStyle) {
     return element.currentStyle[cssPropertyName];
   }
   else {
     return '';
   }
}

function resizeRings() {
	var offset = ($("#hp-content").height() + parseInt($("#hp-content").css('padding-top'))) % 12.0;
	if (offset != 0)
		$("#hp-content").css('padding-bottom', (12.0 - offset) + "px");
	else
		$("#hp-content").css('padding-bottom', '0');
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
		var postContent = document.getElementById( $(this).attr('id') );
		postContent.innerHTML = Autolinker.link(postContent.innerHTML, {
		    twitter: false,
		    email: true,
		    className: "hp-postLink",
		    stripPrefix: false,
		    truncate: 50
		});
	});
	$('.hp-postLink').each(function(){
        if (!$(this).find("span").length)
    	    $(this).append(" <span class='glyphicon glyphicon-share-alt'></span>");
	})
}

$(window).resize(function() {
	resizeRings();
});

resizeRings();
$('[rel="tooltip"]').tooltip();
$('[rel="popover"]').popover();

$('.hp-focus-search').click(function() {
    $('.hp-easy-search').focus();
});

/*
 *  Token
 */
$(document).ready(function () {

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
	 * generate clickable links for post and comments
	 */
	
	autolinkUrls();

	
});

