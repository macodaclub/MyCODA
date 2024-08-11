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