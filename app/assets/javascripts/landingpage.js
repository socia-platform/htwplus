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

function resizeNotepad() {
    $('#hp-feature-demo .hp-notepad-content').css('height', $(window).height() - 160);
    resizeRings();
}

/**
 * Easy Scroll - The page scrolls to the given element under hp-navbar.
 */
function scrollToElement(element) {
    $('html, body').animate({
        scrollTop: $(element).offset().top - $('#hp-navbar').innerHeight()
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

$(window).load(function() {
	// load banner dimensions
    var newImg = new Image();
    newImg.src = 'http://www.htw-berlin.de/fileadmin/HTW/Zentral/DE/HTW/ZR1_Presse/Pressefotos/130719___Philipp_Meuser_0011_01_1200px_crop.jpg';
    imgHeight = newImg.height;
    imgWidth = newImg.width;

	// apply scrollspy for sections, will activate items on hp-navbar if corresponding section is reached while scrolling
    $('.hp-section').each(function() {
        var curY = $(this).offset().top - $('#hp-navbar').outerHeight();
        $(this).scrollspy({
            min: curY,
            max: curY + $(this).innerHeight(),
            onEnter: function(element) {
                $("#hp-navbar li[data-scrollspy-target='"+element.id+"']").addClass('active');
            },
            onLeave: function(element) {
                $("#hp-navbar li[data-scrollspy-target='"+element.id+"']").removeClass('active');
            }
        });
    });
    $('#hp-navbar').addClass('hp-animate');
    resizeBackground();
    resizeNotepad();
});

/**
 * Magic Scroll
 */
var controller;
var demoScene = null;
var scenes = [];
var tweens = [];

function controlAnimation() {
    var mq = window.matchMedia("(min-width: 992px)");
    if (mq.matches) {
        if (!controller.enabled()) {
            controller.enabled(true);
        }
    } else {
        controller.enabled(false);
        $("[id^=hp-feature-text-]").removeAttr('style');
    }
}

function updateScenes(sceneList, tweenList) {
	demoScene.triggerHook(50 / $(window).height());
}

function buildScenes() {
    // init controller
    controller = new ScrollMagic();

    // build scenes
    demoScene = new ScrollScene({triggerElement: "#hp-feature-trigger-demo"})
        .addTo(controller)
        //.addIndicators()
        .setPin("#hp-feature-demo")
        .triggerHook(50 / $(window).height());

    var features = ['login', 'friends', 'groups', 'courses', 'filemgmt', 'newsstream', 'notifications'];

    for (i = 0; i <= features.length; i++) {
        var newTop = ($(window).height() - $(('#hp-feature-text-').concat(features[i])).height()) / 2;
        var tween;
        if (i == 0) {
            tween = new TimelineMax()
                .add(TweenMax.to(('#hp-feature-text-').concat(features[i]), 0.5, {top: newTop}), 0)
                .add(TweenMax.to(('#hp-feature-demo-').concat(features[i]), 0.3, {display: 'block', opacity: 1}), 0);
        } else {
            tween = new TimelineMax()
                //.add(TweenMax.to(('#hp-feature-text-').concat(features[i-1]), 0.5, {top: -200}), 0)
                //.add(TweenMax.to(('#hp-feature-demo-').concat(features[i-1]), 0.3, {opacity: 0, zIndex: 1000}), 0)
                .add(TweenMax.to(('#hp-feature-text-').concat(features[i]), 0.5, {top: newTop}), 0.1)
                .add(TweenMax.to(('#hp-feature-demo-').concat(features[i]), 0.3, {display: 'block', opacity: 1}), 0.3);
        }
        tweens[tweens.length] = tween;
        scenes[scenes.length] = new ScrollScene({triggerElement: ('#hp-feature-trigger-').concat(features[i])})
            .addTo(controller)
            //.addIndicators()
            .setTween(tween);
    }
}


$(document).ready(function() {
    buildScenes();
    controlAnimation();
});

$(window).resize(function() {
	resizeBackground();
	resizeNotepad();
    controlAnimation();
	updateScenes(scenes, tweens);
});

navbarVisibility();