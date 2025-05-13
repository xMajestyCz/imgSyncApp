import { Injectable } from '@angular/core';
import { Firestore, addDoc, collection } from '@angular/fire/firestore';
import { Timestamp } from '@firebase/firestore';
import { Preferences } from '@capacitor/preferences';

@Injectable({
  providedIn: 'root'
})
export class FirestoreService {
  constructor(private firestore: Firestore) {}

  async savePost(data: { description: string, imageUrl: string }) {
    try {
      // Guardar en Firestore
      const docRef = await addDoc(collection(this.firestore, 'posts'), {
        description: data.description,
        imageUrl: data.imageUrl,
        createdAt: Timestamp.now()
      });

      // ¡CORREGIR ESTA PARTE! Usar 'widget_data' en lugar de 'last_post'
      await Preferences.set({
        key: 'widget_data', // ← Cambiar esto
        value: JSON.stringify({
          description: data.description,
          imageUrl: data.imageUrl,
          timestamp: Date.now()
        })
      });

      await Preferences.set({
        key: 'last_post', // Mantener compatibilidad con versión anterior
        value: JSON.stringify({
          description: data.description,
          imageUrl: data.imageUrl,
          timestamp: Date.now()
        })
      });

      console.log('Datos guardados para widget:', { 
        description: data.description,
        imageUrl: data.imageUrl 
      });

      return docRef.id;
    } catch (error) {
      console.error('Error saving to Preferences:', error);
      throw error;
    }
  }
  
}