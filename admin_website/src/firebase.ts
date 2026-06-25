import { initializeApp } from "firebase/app";
import { getFirestore } from "firebase/firestore";
import { getAuth, GoogleAuthProvider } from "firebase/auth";

const firebaseConfig = {
  apiKey: "AIzaSyBS2YjyAJbXt5WWFvaHIjQyZGWIvCnD7R0",
  projectId: "food-app-6b3bc",
  storageBucket: "food-app-6b3bc.firebasestorage.app",
  appId: "1:1019759569001:android:2f5df8304384cc7fb81af8",
  authDomain: "food-app-6b3bc.firebaseapp.com"
};

const app = initializeApp(firebaseConfig);
export const db = getFirestore(app);
export const auth = getAuth(app);
export const googleProvider = new GoogleAuthProvider();

// Global active branch ID for Phase 1 testing
export const ACTIVE_BRANCH_ID = "branch_pia";

