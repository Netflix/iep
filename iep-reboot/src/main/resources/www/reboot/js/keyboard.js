var nf = typeof nf !== 'undefined' ? nf : {};

nf.keyboard = (function() {

    'use strict';

    var keyDownCode = 40,
        keyUpCode = 38,
        keyEnter = 13,
        keyFCode = 70;

    var fireChange = function(element) {

        var changeEvent = new Event('change');
        element.dispatchEvent(changeEvent);

    };

    var bindInputSelect = function(input, select) {

        input.autocomplete = 'off';

        input.addEventListener('keyup', function(e) {

            if (e.keyCode === keyDownCode) {

                select.focus();

                if (select.selectedIndex === -1 && select.options.length) {
                    select.options[0].selected = true;
                    fireChange(select);
                }

            }

        }, false);

        select.addEventListener('keydown', function(e) {

            if (!e.shiftKey && e.keyCode === keyUpCode && this.selectedIndex === 0) {

                input.focus();

                if (select.options.length) {

                    window.setTimeout(function() {

                        select.options[0].selected = false;
                        fireChange(select);

                    }, 0);

                }

            }

        }, false);

        select.addEventListener('keyup', function(e) {

            if (e.ctrlKey && e.keyCode === keyFCode) {

                input.focus();

            } else if (e.keyCode === keyEnter) {

                var event = new CustomEvent('keyboardSelection', {
                    'detail': {
                        'selected': nf.multiSelect.getValuesAsAnArray(select)
                    }
                });

                select.dispatchEvent(event);

                input.focus();

            }

        }, false);

    };

    return {
        bindInputSelect: bindInputSelect
    };

}());