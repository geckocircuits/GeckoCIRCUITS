export const Brightness = function (imageData) {
    const brightness = this.brightness(), data = imageData.data, len = data.length;
    for (let i = 0; i < len; i += 4) {
        data[i] = Math.min(255, data[i] * brightness);
        data[i + 1] = Math.min(255, data[i + 1] * brightness);
        data[i + 2] = Math.min(255, data[i + 2] * brightness);
    }
};
