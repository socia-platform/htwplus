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


$(window).load(function() {
	// load banner dimensions
    var newImg = new Image();
    newImg.src = 'http://www.htw-berlin.de/fileadmin/HTW/Zentral/DE/HTW/ZR1_Presse/Pressefotos/130719___Philipp_Meuser_0011_01_1200px_crop.jpg';
    imgHeight = newImg.height;
    imgWidth = newImg.width;

	// append scrollspy for sections, will activate items on hp-navbar if there are reached while scrolling
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

    resizeBackground();
});

$(window).resize(resizeBackground);