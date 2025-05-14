import { Component, ViewChild } from '@angular/core';
import { FirestoreService } from '../core/services/firestore.service';
import { AlertController, LoadingController, ActionSheetController, IonModal } from '@ionic/angular';
import { SupabaseService } from '../core/services/supabase.service';
import { environment } from 'src/environments/environment';
import { CameraSource } from '@capacitor/camera';
import { CameraService } from '../core/services/camera.service';
import { Capacitor } from '@capacitor/core';
import { Preferences } from '@capacitor/preferences';

@Component({
  selector: 'app-home',
  templateUrl: 'home.page.html',
  styleUrls: ['home.page.scss'],
  standalone: false
})
export class HomePage {
  @ViewChild('listModal') listModal!: IonModal;
  @ViewChild('viewModal') viewModal!: IonModal;
  
  description = '';
  selectedImage: string | null = null;
  fileToUpload: File | null = null;
  isLoading = false;
  posts: any[] = [];
  filteredPosts: any[] = [];
  searchTerm = '';
  viewedImageUrl: string = '';
  viewedImageDescription: string = '';

  constructor(
    private firestoreService: FirestoreService,
    private supabaseService: SupabaseService,
    private cameraService: CameraService,
    private alertController: AlertController,
    private loadingController: LoadingController,
    private actionSheetController: ActionSheetController
  ) {}

  async loadPosts() {
    try {
      this.posts = await this.firestoreService.getPosts();
      this.filteredPosts = [...this.posts];
    } catch (error) {
      console.error('Error cargando imagenes:', error);
      await this.showAlert('Error', 'No se pudieron cargar las imagenes');
    }
  }

  filterPosts(event: any) {
    this.searchTerm = event.target.value.toLowerCase();
    this.filteredPosts = this.posts.filter(post => 
      post.description.toLowerCase().includes(this.searchTerm)
    );
  }

  async deletePost(postId: string, event: Event) {
    event.stopPropagation();
    
    const alert = await this.alertController.create({
      header: 'Confirmar',
      message: '¿Estás seguro de que quieres eliminar esta publicación?',
      buttons: [
        {
          text: 'Cancelar',
          role: 'cancel'
        },
        {
          text: 'Eliminar',
          handler: async () => {
            const loading = await this.loadingController.create({
              message: 'Eliminando publicación...'
            });
            await loading.present();
            
            try {
              await this.firestoreService.deletePost(postId);
              await this.loadPosts();
            } catch (error) {
              await this.showAlert('Error', 'No se pudo eliminar la publicación');
            } finally {
              await loading.dismiss();
            }
          }
        }
      ]
    });

    await alert.present();
  }

  async viewPost(post: any) {
    this.viewedImageUrl = post.imageUrl;
    this.viewedImageDescription = post.description;
    await this.listModal.dismiss();
    await this.viewModal.present();
  }
  
  async selectImageSource() {
    const actionSheet = await this.actionSheetController.create({
      header: 'Seleccionar Imagen',
      buttons: [
        {
          text: 'Tomar Foto',
          icon: 'camera',
          handler: () => this.captureImage(CameraSource.Camera)
        },
        {
          text: 'Elegir de Galería',
          icon: 'image',
          handler: () => this.captureImage(CameraSource.Photos)
        },
        {
          text: 'Cancelar',
          icon: 'close',
          role: 'cancel'
        }
      ]
    });
    await actionSheet.present();
  }

  async captureImage(source: CameraSource) {
    try {
      if (!Capacitor.isNativePlatform()) {
        await this.showAlert('Error', 'Esta función solo está disponible en dispositivos móviles');
        return;
      }

      const photo = await this.cameraService.takePhoto(source);
      console.log('Photo object:', photo); 
      
      if (!photo.webPath) {
        throw new Error('No se pudo obtener la imagen');
      }

      this.fileToUpload = await this.cameraService.convertPhotoToFile(photo);
      this.selectedImage = photo.webPath;

    } catch (error) {
      console.error('Error en captureImage:', error);
      
      let errorMessage = 'No se pudo completar la acción';
      if ((error as Error).message.includes('permission')) {
        errorMessage = 'Permisos insuficientes. Por favor, concede los permisos necesarios en la configuración del dispositivo.';
      } else if ((error as Error).message.includes('No se pudo obtener')) {
        errorMessage = 'No se pudo obtener la imagen seleccionada';
      }
      
      await this.showAlert('Error', errorMessage);
    }
  }

  async savePost() {
    if (!this.fileToUpload || !this.description.trim()) {
      await this.showAlert('Error', 'Por favor completa todos los campos');
      return;
    }

    const loading = await this.loadingController.create({
      message: 'Guardando imagen...'
    });
    
    try {
      await loading.present();

      const imageUrl = await this.supabaseService.uploadImage(
        environment.supabaseBucket, 
        this.fileToUpload
      );

      await this.firestoreService.savePost({
        description: this.description,
        imageUrl
      });

      await loading.dismiss();
      await this.showAlert('Éxito', 'Imagen guardada correctamente');
      this.resetForm();

    } catch (error) {
      await loading.dismiss();
      console.error('Error al guardar:', error);
      await this.showAlert('Error', 'No se pudo guardar la imagen');
    }
  }

  removeImage(event: Event) {
    event.stopPropagation();
    this.selectedImage = null;
    this.fileToUpload = null;
  }

  private resetForm() {
    this.description = '';
    this.selectedImage = null;
    this.fileToUpload = null;
  }

  private async showAlert(header: string, message: string) {
    const alert = await this.alertController.create({
      header,
      message,
      buttons: ['OK']
    });
    await alert.present();
  }

  async refreshWidgetData() {
    try {
      const posts = await this.firestoreService.getAllPostsForWidget();
      
      await Preferences.set({
        key: 'widget_data',
        value: JSON.stringify({
          posts: posts,
          lastUpdate: Date.now()
        })
      });
      
    } catch (error) {
      console.error('Error updating widget data:', error);
    }
  }
}