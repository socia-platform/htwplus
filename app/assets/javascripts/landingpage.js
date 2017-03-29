var imgHeight;
var imgWidth;

/**
 * Maximize the banner image for login area.
 *
 * By default, the banner will be fitted to the width of login area.
 * However, if the height of the banner is too low for login area it will be fitted to the height.
 */
function resizeBackground() {
	if ((imgHeight / imgWidth * $('div#hp-login').innerWidth()) < $('div#hp-login').innerHeight()) {
		$('div#hp-login').css('background-size', 'auto 100%');
	} else {
		$('div#hp-login').css('background-size', '100% auto');
	}
}

/**
 * Easy Scroll - The page scrolls to the given element under hp-navbar.
 */
function scrollToElement(element) {
    $('html, body').animate({
        scrollTop: $(element).offset().top - $('#hp-navbar').innerHeight() + 1
	}, 500);
}

/**
 * Append bootstrap affix class to hp-navbar.
 * Navbar becomes position fixed if login area scrolls out.
 */
$('#hp-navbar').affix({
    offset: {
        top: function () { return $('#hp-login').outerHeight(); }
    }
});

function navbarVisibility() {
    var container = $('div.hp-navbar-container');
    if (container.css('visibility') == 'visible') {
        container.css('display', 'block');
    } else {
        container.css('display', 'none');
    }
}

$('#hp-navbar').on('webkitTransitionEnd', navbarVisibility);
$('#hp-navbar').on('transitionend', navbarVisibility);
$('#hp-navbar').on('otransitionend', navbarVisibility);
$('#hp-navbar').on('MSTransitionEnd', navbarVisibility);

/**
 * Magic Scroll
 */
var controller;
var demoScene = null;
var textScene = null;
var scenes = [];

function controlAnimation() {
    var mq = window.matchMedia("(min-width: 992px)");
    if (mq.matches) {
        if (!controller.enabled()) {
            controller.enabled(true);
            demoScene.refresh();
            textScene.refresh();
        }
    } else {
        if (controller.enabled())
            controller.enabled(false);
    }
}

function updateScenes() {
	demoScene.triggerHook(50 / $(window).height());
	textScene.triggerHook(50 / $(window).height());
}

function resizeScenes() {
    $('#hp-feature-text').css('height', $(window).height() - 110);
    $('#hp-feature-demo .hp-notepad-content').css('height', $(window).height() - 160);
    //resizeRings();
}

/**
 * window listener & main stuff
 */
$(window).on("load", function() {
	// load banner dimensions (aspect ratio will be needed)
    var newImg = new Image();
    //newImg.src = window.location.origin.toString() + '/assets/images/LandingpageBackground/default_medium.jpg';
    //imgHeight = newImg.height;
    //imgWidth = newImg.width;
    imgWidth = 4;
    imgHeight = 3;

	// apply scrollspy for sections, will activate items on hp-navbar if corresponding section is reached while scrolling
    $('.hp-section').each(function() {
        var curY = $(this).offset().top - parseFloat($(this).css('margin-top')) - 50;
        var name = $(this).attr('data-scrollspy-name');
        $(this).scrollspy({
            min: curY,
            max: curY + $(this).outerHeight(true),
            onEnter: function(element) {
                $("#hp-navbar li[data-scrollspy-target='" + $(element).attr('data-scrollspy-name') + "']").addClass('active');
            },
            onLeave: function(element) {
                $("#hp-navbar li[data-scrollspy-target='" + $(element).attr('data-scrollspy-name') + "']").removeClass('active');
            }
        });
    });
    $('#hp-navbar').addClass('hp-animate');
    navbarVisibility();
    resizeBackground();
});

$(window).resize(function() {
	resizeBackground();
	resizeScenes();
});

