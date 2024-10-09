import {
    toRaw,
    isRef,
    isReactive,
    isProxy,
} from 'vue';

export function substringAfter(value, delimiter) {
    value = value || "";

    if (value === "") {
        return value;
    }

    const substrings = value.split(delimiter);

    return substrings.length === 1
        ? this // delimiter is not part of the string
        : substrings.slice(1).join(delimiter);
}

export function nonNulls(obj) {
    return Object.fromEntries(Object.entries(obj).filter(([_, v]) => v != null));
}

export function generateIriSuffix(type) {
    const hexadecimalDigits = '0123456789abcdef';

    function getRandomHexString(length) {
        let result = '';
        for (let i = 0; i < length; i++) {
            result += hexadecimalDigits.charAt(Math.floor(Math.random() * hexadecimalDigits.length));
        }
        return result;
    }

    return [
        type,
        getRandomHexString(8),
        getRandomHexString(4),
        getRandomHexString(4),
        getRandomHexString(4),
        getRandomHexString(12)
    ].join('_');
}

export function deepToRaw(sourceObj) {
    const objectIterator = (input) => {
        if (Array.isArray(input)) {
            return input.map((item) => objectIterator(item));
        }
        if (isRef(input) || isReactive(input) || isProxy(input)) {
            return objectIterator(toRaw(input));
        }
        if (input && typeof input === 'object') {
            return Object.keys(input).reduce((acc, key) => {
                acc[key] = objectIterator(input[key]);
                return acc;
            }, {});
        }
        return input;
    };

    return objectIterator(sourceObj);
}

export function camelCaseToCapitalized(str) {
    return str.replace(/([a-z])([A-Z])/g, '$1 $2').replace(/^./, char => char.toUpperCase());
}

export const delay = timeMillis => new Promise(res => setTimeout(res, timeMillis));