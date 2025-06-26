# Project: Bluetooth-Based Location and Communication Features
This repository contains a comprehensive Android application that leverages Bluetooth technology to provide three main features: Chat, Find, and Talk. The app is designed to facilitate seamless communication, device discovery, and audio-based interactions between Bluetooth-enabled devices.
## Features

### 1. Chat
The Chat feature allows users to send and receive text messages between paired Bluetooth devices. It includes:
- Secure and insecure connection modes.
- Real-time message exchange.
- User-friendly interface for managing conversations.

### 2. Find
The Find feature helps users locate nearby Bluetooth devices. It includes:
- Scanning for paired and unpaired devices.
- Displaying signal strength (RSSI) as a percentage for better proximity estimation.
- Device filtering to show only relevant devices (e.g., smartphones).
- Smooth signal strength calculation using a moving average.

### 3. Talk
The Talk feature enables audio communication between Bluetooth devices. It includes:
- Real-time audio streaming using the device's microphone and speaker.
- Intuitive UI with a press-and-hold button for speaking.
- Automatic playback of received audio.
- Custom action bar to display the connected device's name.

## Permissions
The app requires the following permissions to function:
- Bluetooth: To enable Bluetooth communication.
- Bluetooth Admin: To manage Bluetooth settings.
- Bluetooth Connect: To connect to paired devices.
- Bluetooth Scan: To discover nearby devices.
- Access Fine Location: To scan for Bluetooth devices.
- Record Audio: To enable the Talk feature.

Ensure these permissions are granted for the app to work correctly.

## How It Works

### Chat
1. Navigate to the Chat feature.
2. Select a device to connect securely or insecurely.
3. Start exchanging messages in real-time.

### Find
1. Navigate to the Find feature.
2. Scan for nearby devices.
3. View paired and unpaired devices with their signal strength.
4. Select a device to connect or interact with.

### Talk
1. Navigate to the Talk feature.
2. Connect to a device.
3. Press and hold the Speak button to send audio.
4. Release the button to stop recording and listen to incoming audio.
## Technical Details

### Bluetooth Communication
- Uses “BluetoothAdapter”, “BluetoothSocket”, and “BluetoothServerSocket” for managing connections.
- Supports both secure (RFCOMM) and insecure connections.

### Signal Strength Calculation
- Implements a moving average window to smooth RSSI values.
- Converts RSSI to a percentage for better user understanding.

### Audio Streaming
- Utilizes “AudioRecord” for capturing audio and “AudioTrack” for playback.
- Ensures low-latency communication for real-time audio.

## Setup and Installation

1. Clone the repository: https://github.com/NigelGH/crisiscomm-repository.git
2. Open the project in Android Studio.
3. Build and run the app on a device with Bluetooth capabilities.



## Screenshots
### Chat Feature
![494578883_1342825997017026_5931419042436946227_n](https://github.com/user-attachments/assets/2083baeb-4d9e-4207-a603-40d78b13ea5b)

### Find Feature
![494579015_3517991865174251_8439872324565175303_n](https://github.com/user-attachments/assets/cc31a8cb-1803-462d-8356-8a5971ae7b07)

### Talk Feature
![494579015_1384486322693568_5900913320276688923_n](https://github.com/user-attachments/assets/7f26e675-af2b-400e-9875-a908eac9819e)



## Contributing
Contributions are welcome! Feel free to open issues or submit pull requests to improve the app.

## License
This project is licensed under the MIT License.

## Contact
For any questions or feedback, please contact [NigelGH](https://github.com/NigelGH).



