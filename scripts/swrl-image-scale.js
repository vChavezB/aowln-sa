document.addEventListener("DOMContentLoaded", function () {
    // Get all grid-container elements
    var gridContainers = document.querySelectorAll(".grid-container");

    // Set your maximum allowed height here
    var maxHeight = 400;

    // Function to perform height calculations and styling
    function calculateAndApplyStyles(textElement, bodyElement, headElement, scaleFactor) {
        // Apply styles to the text, body, and head elements
        textElement.style.color = "purple"; // Example: Set text color

        // Scale both bodyElement and headElement to the same height
        bodyElement.style.height = bodyElement.clientHeight * scaleFactor + "px";
        bodyElement.style.width = "auto";
        headElement.style.height = headElement.clientHeight * scaleFactor + "px";
		headElement.style.width = "auto";
    }

    // Iterate through each grid container
    gridContainers.forEach(function (gridContainer) {
        // Get the text, body, and head elements within the current grid container
        var textElement = gridContainer.querySelector("div");
        var bodyElement = gridContainer.querySelector("#body");
        var headElement = gridContainer.querySelector("#head");

        // Skip to the next iteration if bodyElement is null
        if (!bodyElement) {
            return;
        }

        // Counter for tracking the number of images loaded
        var imagesLoaded = 0;

        // Function to check if both images are loaded
        function checkImagesLoaded() {
            imagesLoaded++;

            // If both images are loaded, perform calculations and apply styles
            if (imagesLoaded === 2) {
                // Calculate the height of the body and head images
                var bodyHeight = bodyElement.clientHeight;
                var headHeight = headElement.clientHeight;

                // Determine the scaling factor based on the taller image
                var scaleFactor = maxHeight / Math.max(bodyHeight, headHeight);

                calculateAndApplyStyles(textElement, bodyElement, headElement, scaleFactor);
            }
        }

        // Wait for the body image to load
        bodyElement.onload = checkImagesLoaded;

        // Wait for the head image to load (if it exists)
        if (headElement) {
            headElement.onload = checkImagesLoaded;
        }
    });
});