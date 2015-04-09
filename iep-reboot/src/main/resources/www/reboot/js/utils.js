var nf = typeof nf !== 'undefined' ? nf : {};

nf.utils = (function() {

    // createRow is an attempt to get around these issues:
    // https://github.com/Polymer/polymer/issues/671
    // https://github.com/Polymer/TemplateBinding/issues/57
    var createRow = function(data, isSafeHTML, isHeader) {

        var tr = document.createElement('tr');

        data.forEach(function(item) {

            var nodeType = isHeader ? 'th' : 'td',
                td = document.createElement(nodeType);

            if (item instanceof HTMLElement || item instanceof DocumentFragment) {

                td.appendChild(item);

            } else if (isSafeHTML) {

                td.innerHTML = item;

            } else {

                td.textContent = item;

            }

            tr.appendChild(td);

        });

        return tr;

    };

    var arrayToSet = function(array) {

        var set = new Set();

        // new Set(iterable) is not supported in Safari yet
        array.forEach(function(item) {
            set.add(item);
        });

        return set;

    };

    var setToArray = function(set) {

        var array = [];

        set.forEach(function(item) {
            array.push(item);
        });

        return array;

    };

    var extend = function ( objects ) {

        var extended = {};

        var merge = function (obj) {
            for (var prop in obj) {
                if (Object.prototype.hasOwnProperty.call(obj, prop)) {
                    extended[prop] = obj[prop];
                }
            }
        };

        merge(arguments[0]);

        for (var i = 1; i < arguments.length; i++) {
            var obj = arguments[i];
            merge(obj);
        }

        return extended;

    };

    return {

        createRow: createRow,
        arrayToSet: arrayToSet,
        setToArray: setToArray,
        extend: extend

    }

}());