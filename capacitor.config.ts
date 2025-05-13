import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.imgSync.app',
  appName: 'imgSync',
  webDir: 'www',
  plugins: {
    Camera: {
      allowEditing: true, 
      saveToGallery: false,
      correctOrientation: true,
      presentationStyle: 'fullscreen',
      androidAllowMixedContent: true,
      intentAction: 'android.media.action.IMAGE_CAPTURE'
    }
  },
  android: {
    allowMixedContent: true,
    overrideUserAgent: 'Mozilla/5.0 Google'
  }
};

export default config;