var nf = typeof nf !== 'undefined' ? nf : {};

nf.sync = (function() {

    'use strict';

    var dirtyCheckKeyword = '__isDirty__',
        type = {
            input         : 'input',
            select        : 'select',
            multipleSelect: 'multipleSelect'
        };

    var replicateChanges = function(sourceElement, recipientElement, elementType, listenToKeyChanges) {

        var eventName = listenToKeyChanges ? 'keyup' : 'change';

        sourceElement.addEventListener(eventName, function() {

            var changeEvent;

            if (this[dirtyCheckKeyword]) {

                this[dirtyCheckKeyword] = false;

            } else {

                changeEvent = new Event(eventName, {bubbles: true});

                pushElementValue(sourceElement, recipientElement, elementType);

                recipientElement[dirtyCheckKeyword] = true;

                recipientElement.dispatchEvent(changeEvent);

                if (changeEvent === 'keyup') {
                    //required for Angular 1.x
                    recipientElement.dispatchEvent('input');
                }

            }

        });

        if (listenToKeyChanges) {
            // calling again without 'listeningToKeyChanges' to register change events too
            replicateChanges.apply(this, [].slice.call(arguments, 0, arguments.length - 1) );
        }

    };

    var getFormElementType = function(element) {

        var elementType;

        if (element.tagName === 'INPUT') {

            elementType = type.input;

        } else if (element.tagName === 'SELECT' && element.multiple) {

            elementType = type.multipleSelect;

        } else if (element.tagName === 'SELECT') {

            elementType = type.select;

        }
        return elementType;

    };

    var watchForMutations = function(sourceElement, recipientElement, emitOnBack) {

        var observer = new MutationObserver(function() {

            if (sourceElement[dirtyCheckKeyword]) {

                sourceElement[dirtyCheckKeyword] = false;

            } else {

                recipientElement[dirtyCheckKeyword] = true;

                pushMultiSelectState(sourceElement, recipientElement, emitOnBack);

            }

        });

        observer.observe(sourceElement, {attributes: false, childList: true, characterData: false});

    };

    var sync = function(elementA, elementB, emitOnBack, listenToKeyChanges) {

        var elementType = getFormElementType(elementA);

        if (elementType === type.multipleSelect) {

            watchForMutations(elementA, elementB);
            watchForMutations(elementB, elementA, emitOnBack);

        }

        if (elementType) {

            var changeEvent;

            replicateChanges(elementA, elementB, elementType, listenToKeyChanges);
            replicateChanges(elementB, elementA, elementType, listenToKeyChanges);

            if (elementA.value && elementA.value.length) {

                changeEvent = new Event('change', {bubbles: true});
                elementB.value = elementA.value;
                elementB.dispatchEvent(changeEvent);

            }

        } else {

            console.error('The type of this form element is not currently supported');

        }

    };

    // copy all options and their states from one select to another
    var pushMultiSelectState = function(sourceElement, recipientElement, emitOnBack) {

        if (emitOnBack) {

            var event = new CustomEvent("change:selectOnly", {
                "detail":{"selected":nf.multiSelect.getValuesAsAnArray(sourceElement)}
            });

            sourceElement.dispatchEvent(event);

        } else {

            var docFrag = document.createDocumentFragment(),
                options = sourceElement.options,
                option,
                newOption,
                i, l;

            for (l = options.length, i = 0; i < l; i++) {
                option = options[i];
                newOption = document.createElement('option');
                newOption.textContent = option.textContent;
                newOption.value = option.value;
                newOption.disabled = option.disabled;
                newOption.selected = option.selected;
                docFrag.appendChild(newOption);
            }

            recipientElement.innerHTML = '';
            recipientElement.appendChild(docFrag);

        }

    };

    var pushMultiSelectValue = function(sourceElement, recipientElement) {

        var values = nf.multiSelect.getValuesAsAnArray(sourceElement);
        nf.multiSelect.setSelectedValues(recipientElement, values, true);

    };

    var pushElementValue = function(sourceElement, recipientElement, elementType) {

        switch (elementType) {

            case type.input:
                recipientElement.value = sourceElement.value;
                break;
            case type.select:
                recipientElement.value = sourceElement.value;
                break;
            case type.multipleSelect:
                pushMultiSelectValue(sourceElement, recipientElement);
                break;

        }

    };

    return {
        sync: sync
    };

}());