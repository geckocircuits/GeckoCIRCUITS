export const Grayscale = function (imageData) {
    const data = imageData.data, len = data.length;
    for (let i = 0; i < len; i += 4) {
        const brightness = 0.34 * data[i] + 0.5 * data[i + 1] + 0.16 * data[i + 2];
        data[i] = brightness;
        data[i + 1] = brightness;
        data[i + 2] = brightness;
    }
};
