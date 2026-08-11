import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Navbar from './components/Navbar';
import Login from './pages/Login';
import Signup from './pages/Signup';
import FarmerDashboard from './pages/FarmerDashboard';
import OfficerDashboard from './pages/OfficerDashboard';
import AdminDashboard from './pages/AdminDashboard';
import NotFound from './pages/NotFound';
import FarmList from './components/farms/FarmList';
import FarmCreate from './components/farms/FarmCreate';
import AdvisoryRequests from './pages/farmer/AdvisoryRequests';
import SoilReadingList from './components/soil/SoilReadingList';
import SoilReadingForm from './components/soil/SoilReadingForm';
import AdvisoryForm from './components/advisory/AdvisoryForm';

function HomeRedirect() {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" />;
  switch (user.role) {
    case 'FARMER': return <Navigate to="/farmer" />;
    case 'OFFICER': return <Navigate to="/officer" />;
    case 'ADMIN': return <Navigate to="/admin" />;
    default: return <Navigate to="/login" />;
  }
}

export default function App() {
  return (
    <AuthProvider>
      <Router>
        <Navbar />
        <Routes>
          {/* Public routes */}
          <Route path="/login" element={<Login />} />
          <Route path="/signup" element={<Signup />} />
          
          {/* Role-based protected routes */}
          <Route element={<ProtectedRoute allowedRoles={['FARMER']} />}>
            <Route path="/farmer" element={<FarmerDashboard />}>
              <Route index element={<FarmList />} />
              <Route path="create-farm" element={<FarmCreate />} />
              <Route path="advisory-requests" element={<AdvisoryRequests />} />
              <Route path="farms/:farmId/soil-readings" element={<SoilReadingList />} />
              <Route path="farms/:farmId/soil-readings/new" element={<SoilReadingForm />} />
              <Route path="advisory-requests/new" element={<AdvisoryForm />} />
            </Route>
          </Route>
          <Route element={<ProtectedRoute allowedRoles={['OFFICER']} />}>
            <Route path="/officer" element={<OfficerDashboard />} />
          </Route>
          <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
            <Route path="/admin" element={<AdminDashboard />} />
          </Route>
          
          {/* Catch-all */}
          <Route path="/" element={<HomeRedirect />} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}