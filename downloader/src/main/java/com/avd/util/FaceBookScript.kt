package com.avd.util

object FaceBookScript {

    val jsCode = """
    javascript:(function() {
    function addDownloadButton(element) {
        if (element.getAttribute('data-has-button')) {
            console.log('Button already added to element:', element);
            return; // Button already added
        }
        element.setAttribute('data-has-button', 'true');

        let button = document.createElement('button');

        // Create an inner div for the icon
        let iconDiv = document.createElement('div');
        iconDiv.style.width = '100%';
        iconDiv.style.height = '100%';
        iconDiv.style.display = 'flex';
        iconDiv.style.alignItems = 'center';
        iconDiv.style.justifyContent = 'center';
        iconDiv.innerHTML = `
           <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 8 12" width="15" height="26">
                <path d="M3.954,9.939C4.023,10.008 4.114,10.042 4.205,10.042C4.295,10.042 4.386,10.008 4.456,9.939L7.306,7.089C7.407,6.987 7.437,6.835 7.382,6.702C7.328,6.569 7.198,6.483 7.055,6.483H5.984V3.204C5.984,3.008 5.826,2.85 5.63,2.85H2.78C2.584,2.85 2.425,3.008 2.425,3.204V6.483H1.355C1.211,6.483 1.082,6.569 1.027,6.702C0.972,6.835 1.002,6.987 1.104,7.089L3.954,9.939Z" fill="#ffffff"/>
                <path d="M2.78,2.133H5.63C5.825,2.133 5.984,1.975 5.984,1.779C5.984,1.583 5.825,1.424 5.63,1.424H2.78C2.584,1.424 2.425,1.583 2.425,1.779C2.425,1.975 2.584,2.133 2.78,2.133Z" fill="#ffffff"/>
                <path d="M2.78,0.71H5.63C5.825,0.71 5.984,0.551 5.984,0.355C5.984,0.159 5.825,0 5.63,0H2.78C2.584,0 2.425,0.159 2.425,0.355C2.425,0.551 2.584,0.71 2.78,0.71Z" fill="#ffffff"/>
                <path d="M1,11L7.5,11A0.5,0.5 0,0 1,8 11.5L8,11.5A0.5,0.5 0,0 1,7.5 12L1,12A0.5,0.5 0,0 1,0.5 11.5L0.5,11.5A0.5,0.5 0,0 1,1 11z" fill="#ffffff"/>
            </svg>
        `;
        button.style.position = 'absolute';
        button.style.right = '10px';
        button.style.top = '130px';
        button.style.width = '42px';
        button.style.height = '42px';
        button.style.borderRadius = '50%';
        button.style.backgroundColor = '#2AB079'; // Darker green
        button.style.border = 'none';
        button.style.zIndex = '1000';
        button.style.cursor = 'pointer';
        button.style.padding = '0';

        button.onclick = function(event) {
            event.stopPropagation();
            event.preventDefault();

            let videoDiv = element.closest('div[data-video-url]');
            if (videoDiv) {
                let videoUrl = videoDiv.getAttribute('data-video-url');
                if (videoUrl) {
                    console.log('Real video URL:', videoUrl);
                    alert('Real video URL: ' + videoUrl);
                    AndroidInterface.onVideoClicked(videoUrl);
                } else {
                    console.log('No video URL available directly for element:', element);
                    alert('No video URL available directly.');
                }
            } else {
                console.log('No div with data-video-url found for element:', element);
                alert('No div with data-video-url found.');
            }
        };
        button.appendChild(iconDiv);
        element.style.position = 'relative';
        element.appendChild(button);
        console.log('Button added to element:', element);
    }

    const observer = new MutationObserver((mutations) => {
        mutations.forEach((mutation) => {
            mutation.addedNodes.forEach((node) => {
                if (node.nodeType === 1) { // Ensure the added node is an element
                    if (node.matches('div[data-video-id]')) {
                        addDownloadButton(node);
                    } else if (node.querySelectorAll) {
                        const elements = node.querySelectorAll('div[data-video-id]');
                        elements.forEach(addDownloadButton);
                    }
                }
            });
        });
    });

    document.querySelectorAll('div[data-video-id]').forEach(addDownloadButton); // Initial setup for existing elements

    observer.observe(document.body, {
        childList: true,
        subtree: true
    });
})();
""".trimIndent()
}