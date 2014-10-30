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
});

/**
 * Magic Scroll
 */
var controller;
var demoScene = null;
var textScene = null;
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
}

function buildScenes() {
    // init controller
    controller = new ScrollMagic();

    // build scenes
    demoScene = new ScrollScene({triggerElement: "#hp-feature-trigger-main"})
        .addTo(controller)
        .addIndicators()
        .setPin("#hp-feature-demo")
        .triggerHook(50 / $(window).height());
    textScene = new ScrollScene({triggerElement: "#hp-feature-trigger-main"})
        .addTo(controller)
        .addIndicators()
        .setPin("#hp-feature-text")
        .triggerHook(50 / $(window).height());

    var features = ['login', 'friends', 'groups', 'courses', 'filemgmt', 'newsstream', 'notifications'];
    var newTitle, newDescription, oldTitle, oldDescription = '';

    var $title = $('#hp-feature-text .hp-feature-title');
    var $description = $('#hp-feature-text .hp-feature-description');

    for (i = 1; i <= features.length; i++) {
		newTitle = $('#hp-feature-text-' + features[i] + ' .hp-feature-title').html();
		newDescription = $('#hp-feature-text-' + features[i] + ' .hp-feature-description').html();
		oldTitle = $('#hp-feature-text-' + features[i-1] + ' .hp-feature-title').html();
		oldDescription = $('#hp-feature-text-' + features[i-1] + ' .hp-feature-description').html();
        var tween = new TimelineMax()
            .add(TweenMax.fromTo('#hp-feature-text', 0.1, {opacity: 1}, {opacity: 0}), 0)
            .add(TweenMax.fromTo('#hp-feature-text', 0.2, {y: 0}, {y: -500}), 0)
            //.add(TweenMax.fromTo('#hp-feature-text .hp-feature-title', 0.5, {x: 0}, {x: 200}), 0.2)
            .add(TweenMax.fromTo(CSSRulePlugin.getRule('.hp-feature-title:before'), 0, {cssRule:{content: 'blaaa1'}}, {cssRule:{content: 'blaaa2'}}), 0.2)
            //.add(TweenMax.to('#hp-feature-text', 0, {
            //    onReverseComplete:  function() {
            //        $title.html('bla ' + (i-1));
            //        $description.html('blubb ' + (i-1));
            //    },
            //    onStart: function() {
            //        $title.html('bla ' + i);
            //        $description.html('blubb ' + i);
            //    }}), 0.2)
            .add(TweenMax.fromTo('#hp-feature-text', 0.3, {y: 1500}, {y: 0}), 0.2)
            .add(TweenMax.fromTo('#hp-feature-text', 0.1, {opacity: 0}, {opacity: 1}), 0.2)
            .add(TweenMax.to(('#hp-feature-demo-').concat(features[i]), 0.3, {display: 'block', opacity: 1}), 0.2);
        tweens[tweens.length] = tween;
        scenes[scenes.length] = new ScrollScene({triggerElement: ('#hp-feature-trigger-').concat(features[i])})
            .addTo(controller)
            .addIndicators()
            .setTween(tween)
            .triggerHook(1);
    }
    resizeScenes();
}


$(document).ready(function() {
    buildScenes();
    //controlAnimation();
});

$(window).resize(function() {
	resizeBackground();
	updateScenes();
	resizeScenes();
});

navbarVisibility();