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
    resizeRings();
}

function changeText(element) {
	var newTitle = $('#hp-feature-text-' + element + ' .hp-feature-title').html();
	var newDescription = $('#hp-feature-text-' + element + ' .hp-feature-description').html();
	$('#hp-feature-text .hp-feature-title').html(newTitle);
    $('#hp-feature-text .hp-feature-description').html(newDescription);
    return true;
}

function buildScenes() {
    // init controller
    TweenMax.defaultOverwrite = false;
    controller = new ScrollMagic();

    // get all animation items
    var features = [];
    $($("[id^=hp-feature-text-]")).each(function() {
        features[features.length] = $(this).attr('id').replace('hp-feature-text-', '');
    });

    // build scenes - pins for demo and text area
    demoScene = new ScrollScene({triggerElement: "#hp-features-trigger"})
        .addTo(controller)
        //.addIndicators()
        .duration(features.length * 500)
        .setPin("#hp-feature-demo")
        .triggerHook(50 / $(window).height());
    textScene = new ScrollScene({triggerElement: "#hp-features-trigger"})
        .addTo(controller)
        //.addIndicators()
        .duration(features.length * 500)
        .setPin("#hp-feature-text")
        .triggerHook(50 / $(window).height());

    // build scenes - tweens for features
    for (i = 1; i < features.length; i++) {
        var tween = new TimelineMax()
            .add(TweenMax.fromTo('#hp-feature-text', 0.1, {opacity: 1}, {overwrite: false, opacity: 0}), 0)
            .add(TweenMax.fromTo('#hp-feature-text', 0.2, {y: 0}, {overwrite: false, y: -500}), 0)
            .add(TweenMax.fromTo('#hp-feature-text', 0.3, {y: 1500}, {overwrite: false, y: 0,
                onStart: changeText, onStartParams: [features[i]],
                onReverseComplete: changeText, onReverseCompleteParams: [features[i-1]]
            }), 0.2)
            .add(TweenMax.fromTo('#hp-feature-text', 0.1, {opacity: 0}, {overwrite: false, opacity: 1}), 0.2)
            .add(TweenMax.fromTo(('#hp-feature-demo-').concat(features[i]), 0.3, {display: 'none', opacity: 0}, {overwrite: false, display: 'block', opacity: 1}), 0.2);
        scenes[scenes.length] = new ScrollScene({triggerElement: ('#hp-features-trigger')})
            .addTo(controller)
            //.addIndicators()
            .setTween(tween)
            .offset($('#hp-feature-demo').innerHeight() + 500 * (i + 1))
            .triggerHook(1);
    }
    TweenMax.set('#hp-feature-text', {opacity: 1, y: 0,});

    resizeScenes();
}

/**
 * window listener & main stuff
 */
$(window).load(function() {
	// load banner dimensions
    var newImg = new Image();
    newImg.src = 'http://www.htw-berlin.de/fileadmin/HTW/Zentral/DE/HTW/ZR1_Presse/Pressefotos/130719___Philipp_Meuser_0011_01_1200px_crop.jpg';
    imgHeight = newImg.height;
    imgWidth = newImg.width;

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

$(document).ready(function() {
    buildScenes();
});

$(window).resize(function() {
	resizeBackground();
    controlAnimation();
	updateScenes();
	resizeScenes();
});

