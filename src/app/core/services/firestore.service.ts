import { Injectable } from '@angular/core';
import { 
  Firestore, 
  addDoc, 
  collection, 
  deleteDoc, 
  doc, 
  getDocs, 
  orderBy, 
  query 
} from '@angular/fire/firestore';
import { Timestamp } from '@firebase/firestore';
import { Preferences } from '@capacitor/preferences';

@Injectable({
  providedIn: 'root'
})
export class FirestoreService {
  constructor(private firestore: Firestore) {}

  async savePost(data: { description: string, imageUrl: string }) {
    try {
      const postData = {
        description: data.description,
        imageUrl: data.imageUrl,
        createdAt: Timestamp.now()
      };

      const docRef = await addDoc(collection(this.firestore, 'posts'), postData);
      
      await this.updateWidgetPreferences(data.imageUrl, data.description);
      
      return { id: docRef.id, ...postData };
    } catch (error) {
      console.error('Error saving post:', error);
      throw error;
    }
  }

  async getAllPostsForWidget(): Promise<{imageUrl: string, description: string}[]> {
    try {
      const postsCollection = collection(this.firestore, 'posts');
      const q = query(postsCollection, orderBy('createdAt', 'desc'));
      const querySnapshot = await getDocs(q);
      
      return querySnapshot.docs.map(doc => ({
        imageUrl: doc.data()['imageUrl'],
        description: doc.data()['description']
      }));
    } catch (error) {
      console.error('Error getting posts:', error);
      throw error;
    }
  }

  private async updateWidgetPreferences(imageUrl: string, description: string) {
    try {
      const currentPosts = await this.getAllPostsForWidget();
      
      currentPosts.unshift({imageUrl, description});
      
      await Preferences.set({
        key: 'widget_data',
        value: JSON.stringify({
          posts: currentPosts,
          lastUpdate: Date.now()
        })
      });
    } catch (error) {
      console.error('Error updating widget preferences:', error);
      throw error;
    }
  }

  async getPosts() {
    try {
      const postsCollection = collection(this.firestore, 'posts');
      const q = query(postsCollection, orderBy('createdAt', 'desc'));
      const querySnapshot = await getDocs(q);
      
      return querySnapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data(),
        createdAt: doc.data()['createdAt']?.toDate()
      }));
    } catch (error) {
      console.error('Error getting posts:', error);
      throw error;
    }
  }

  async deletePost(postId: string) {
    try {
      await deleteDoc(doc(this.firestore, 'posts', postId));
    } catch (error) {
      console.error('Error deleting post:', error);
      throw error;
    }
  }

}