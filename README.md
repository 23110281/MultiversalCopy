# MultiversalCopy

An Android application that captures screen content and uses a PaddleOCR backend to extract and overlay text.

## 1. Start the Backend
1. Upload the `backend/paddleocr_api_1.ipynb` notebook to Google Colab.
2. Run all cells to start the PaddleOCR API server.
3. Copy the generated ngrok URL from the output.

## 2. Install the App
1. Go to the **Releases** tab on this GitHub repository.
2. Download and install the latest `app-debug.apk` on your Android device.

## 3. Connect and Use
1. Open MultiversalCopy on your phone.
2. Tap the **Settings** icon (gear) in the top right corner.
3. Paste your ngrok URL into the API Endpoint field and hit Save.
4. Tap **Start Capture**. You can now use the on-screen overlay to extract text.
