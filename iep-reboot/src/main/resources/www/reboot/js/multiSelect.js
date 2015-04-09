var nf = typeof nf !== 'undefined' ? nf : {};

nf.multiSelect = (function() {

    'use strict';

    var getValuesAsAnArray = function(s) {

        var valueArray = [];

        [].forEach.call(s.selectedOptions, function(option) {
            valueArray.push(option.value);
        }, false);

        return valueArray;

    };

    var clearSelectedOptions = function(select, muteChangeEvent) {

        var changeEvent,
            itDidChange = false;

        while(select.selectedOptions.length) {
            itDidChange = true;
            select.selectedOptions[0].selected = false;
        }

        if (!muteChangeEvent && itDidChange) {
            changeEvent = new Event('change');
            select.dispatchEvent(changeEvent);
        }

    };

    // returns array of all values from a select node
    var toArray = function(select) {

        var arrayOfValues = [],
            options = select.options;

        [].forEach.call(options, function(option) {

            arrayOfValues.push(
                option.value
            );

        });

        return arrayOfValues;

    };

    var setSelectedValues = function(select, arrayOfValuesToSelect, muteChangeEvent) {

        var changeEvent,
            arrayOfValuesWithSelectPending = arrayOfValuesToSelect.slice(),
            options = select.options,
            arrayOfOptionValues,
            i,
            l;

        arrayOfOptionValues = toArray(select);

        clearSelectedOptions(select, true);

        for (i = 0, l = arrayOfOptionValues.length;
             i < l && arrayOfValuesWithSelectPending.length; //loop until you find all values to select
             i++) {

            //value matches existing option, marking it as selected
            if (arrayOfValuesToSelect.indexOf(arrayOfOptionValues[i]) !== -1) {

                options[i].selected = true;

                //removing relevant entry from arrayOfValuesWithSelectPending
                arrayOfValuesWithSelectPending.splice(
                    arrayOfValuesWithSelectPending.indexOf(arrayOfOptionValues[i]),
                    1
                );

            }

        }

        if (!muteChangeEvent) {
            changeEvent = new Event('change');
            select.dispatchEvent(changeEvent);
        }

    };

    return {
        getValuesAsAnArray:getValuesAsAnArray,
        clearSelectedOptions: clearSelectedOptions,
        toArray:toArray,
        setSelectedValues:setSelectedValues
    };

}());