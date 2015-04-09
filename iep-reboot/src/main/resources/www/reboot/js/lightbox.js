var nf = typeof nf !== 'undefined' ? nf : {};

nf.lightbox = (function() {

    'use strict';

    var htmlTag = document.querySelector('html'),
        escKeyCode = 27,
        closeLightboxClass = 'nf-lightbox-close',
        node,
        nfLighboxKeydownHandler,
        nfLighboxClickHandler,
        nfLighboxContainerClickHandler;

    var clear = function() {

        if (htmlTag.lightboxOpen) {
            htmlTag.lightboxOpen = false;
            close();
        }

        if (nfLighboxKeydownHandler) {
            htmlTag.removeEventListener('keydown', nfLighboxKeydownHandler, false);
            nfLighboxKeydownHandler = null;
        }

        if (nfLighboxClickHandler) {
            htmlTag.removeEventListener('click', nfLighboxClickHandler, false);
            nfLighboxClickHandler = null;
        }

        if (nfLighboxContainerClickHandler) {
            node.removeEventListener('click', nfLighboxContainerClickHandler, false);
            nfLighboxContainerClickHandler = null;
        }

        node = null;

    };

    var close = function() {

        if (node){
            node.classList.add('is-hidden');
            clear();
        }

    };

    var setup = function(_node) {

        clear();

        node = _node;

        nfLighboxKeydownHandler = function(e) {

            if (e.keyCode === escKeyCode) {
                close();
            }

        };

        nfLighboxContainerClickHandler = function(e) {

            e.stopPropagation();

            if ( e.target.classList.contains(closeLightboxClass) ) {

                close();

            }

        };

        nfLighboxClickHandler = function(e) {

            close();

        };

        htmlTag.addEventListener('keydown', nfLighboxKeydownHandler, false);

        htmlTag.addEventListener('click', nfLighboxClickHandler, false);

        node.addEventListener('click', nfLighboxContainerClickHandler, false);

        node.classList.remove('is-hidden');

        htmlTag.lightboxOpen = true;

    };

    return {

        setup: setup,
        close: close

    }

}());