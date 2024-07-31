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