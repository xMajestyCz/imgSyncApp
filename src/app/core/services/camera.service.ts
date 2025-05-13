import { Injectable } from '@angular/core';
import { Camera, CameraResultType, CameraSource, Photo } from '@capacitor/camera';

@Injectable({
  providedIn: 'root'
})
export class CameraService {
  constructor() {}

  async takePhoto(source: CameraSource): Promise<Photo> {
    try {
      const photo = await Camera.getPhoto({
        quality: 90,
        allowEditing: true, 
        resultType: CameraResultType.Uri, 
        source: source,
        saveToGallery: false,
        correctOrientation: true,
        presentationStyle: 'fullscreen',
        width: 1024,
        height: 1024,
      });

      console.log('Photo taken:', photo);
      return photo;
    } catch (error) {
      console.error('Camera error:', error);
      throw new Error('Error al acceder a la cámara: ' + (error as Error).message);
    }
  }

  async convertPhotoToFile(photo: Photo): Promise<File> {
    try {
      if (!photo.webPath) {
        throw new Error('No se pudo obtener la ruta de la imagen');
      }

      const response = await fetch(photo.webPath);
      if (!response.ok) throw new Error('Error al cargar la imagen');

      const blob = await response.blob();
      const fileName = `img_${Date.now()}.${blob.type.split('/')[1] || 'jpg'}`;
      
      return new File([blob], fileName, { type: blob.type || 'image/jpeg' });
    } catch (error) {
      console.error('Error converting photo:', error);
      throw error;
    }
  }
}