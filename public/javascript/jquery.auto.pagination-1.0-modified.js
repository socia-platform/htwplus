/**
 * jQuery Auto Pagination v1.0 - modified
 * Copyright 2013 Choy Peng Kong
 * An unobstrusive auto pagination plugin for JQuery
 *
 * Inspired by "Infinite Scroll" JQuery Plugin
 * by Paul Irish & Luke Shumard
 *
 * Dual licensed under the MIT and GPL
 *   http://www.opensource.org/licenses/mit-license.php
 *   http://www.gnu.org/licenses/gpl.html
 *
 * !! MODIFIED VERSION !!
 * - uses nextPageSelector.hide() instead of 'visiblity:hidden' -> element doesn't use space on the page
 * - loads new elements from custom page instead of loading the full page again (if  data-raw-link attribute is present, use that one)
 */
(function( $ ) {
    $.fn.AutoPagination = function( options ) {

        var opts = $.extend( {}, $.fn.AutoPagination.defaults, options ),
            $this = $( this );

        // Hide the 'nextPageSelector' anchor since it the point of this
        // plugin is that one shouldn't need to click 'Next Page'
        $( opts.nextPageSelector ).hide();

        // scroll event fires repeatedly as the window is scrolled
        $( window ).scroll(function() {
            $( opts.nextPageSelector ).show(); // nextPage has to be visible, else the position will be 0

            // if 'nextPageSelector' anchor href isn't empty and...
            if ( $( opts.nextPageSelector ).attr( 'href' ) &&
                    // ...window scroll is less then 'nextPageBufferPx' pixels away from it
                $( window ).scrollTop() + $( window ).height() >
                $( opts.nextPageSelector).last().offset().top - opts.nextPageBufferPx ) {

                // remember the 'nextPageSelector' anchor href as 'nextPage'
                var nextPage = $( opts.nextPageSelector ).attr( 'href' );

                // maybe use raw link
                if($( opts.nextPageSelector ).data('raw-page-link')) {
                    nextPage = $( opts.nextPageSelector ).data('raw-page-link');
                }

                // set the 'nextPageSelector' anchor href to an empty string
                $( opts.nextPageSelector ).attr( 'href', '' );

                // add loader div to DOM after last panel
                $( '<div class="'+opts.loaderDivClass+'" style="'+opts.loaderDivStyle+'"><span>'+
                (opts.loaderImage ? '<img src="'+opts.loaderImage+'" alt="'+opts.loaderText+'" />' : opts.loaderText)+
                '</span></div>' ).insertAfter( $this.find( opts.panelSelector ).last() );

                // use ajax 'GET' to grab the 'nextPage' html
                $.get( nextPage, function(data) {

                    var content = $('<div>'+data+'</div>'); // we have to wrap it in another div, because jquery doesn't parse the outer divs somehow...

                    // for each 'panelSelector' element in the loaded data...
                    console.log(opts.panelSelector);
                    window.apdata = data;
                    content.find( opts.panelSelector ).each(function( index, el ) {
                        // ...insert it after the last 'panelSelector'
                        $( el ).insertAfter( $this.find( opts.panelSelector ).last() );
                    });

                    // remove loader div from DOM
                    $( '.'+opts.loaderDivClass ).remove();

                    // reset the 'nextPageSelector' anchor href
                    $( opts.nextPageSelector ).attr( 'href', content.find( opts.nextPageSelector ).attr( 'href' ) );
                    $( opts.nextPageSelector ).data( 'raw-page-link', content.find( opts.nextPageSelector ).data( 'raw-page-link' ) );

                }); // closing of ajax 'GET'

            } // closing of if 'nextPageSelector' anchor...
            $( opts.nextPageSelector ).hide(); // hide the nextPage element again
        }); // closing of scroll event
    };

    $.fn.AutoPagination.defaults = {
        nextPageBufferPx: 200,
        nextPageSelector: '.nextPage',
        panelSelector: '.panel',
        loaderDivClass: 'ajax-loader',
        loaderDivStyle: 'text-align:center',
        loaderImage: 'ajax-loader.gif',
        loaderText: 'Loading...'
    };
})(jQuery);