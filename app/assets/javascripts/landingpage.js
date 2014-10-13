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
function scrollTo(element) {
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

function replaceTriggerHook() {
	if (scene != null)
		scene.triggerHook(50 / $(window).height());
}

$(document).ready(function() {
	// init controller
	controller = new ScrollMagic();

	// build scenes
	var scene0 = new ScrollScene({triggerElement: "#hp-feature-trigger-demo"})
					.setPin("#hp-feature-demo")
					.addTo(controller)
					.triggerHook(50 / $(window).height())
					.addIndicators();

	var scene1 = new ScrollScene({triggerElement: "#hp-feature-trigger-1", duration: 300})
					.setTween(TweenMax.to("#hp-feature-text-1", 0.5, {display: "none"}))
					.addTo(controller)
					.triggerHook(50 / $(window).height())
					.addIndicators();

	var scene2 = new ScrollScene({triggerElement: "#hp-feature-trigger-2", duration: 300})
					.setTween(TweenMax.to("#hp-feature-text-2", 0.5, {display: "none"}))
					.addTo(controller)
					.triggerHook(50 / $(window).height())
					.addIndicators();

});


$(window).resize(function() {
	resizeBackground();
	//replaceTriggerHook();
});

navbarVisibility();