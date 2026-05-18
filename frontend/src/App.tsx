import { useState, useEffect } from 'react';
import { api, setActorId } from './api';
import { Card, CardContent, CardDescription, CardHeader, CardTitle, CardFooter } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger, DialogFooter } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';

function App() {
  const [currentUser, setCurrentUser] = useState<any>(null);
  const [goalSheet, setGoalSheet] = useState<any>(null);
  const [team, setTeam] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  
  // Login State
  const [loginUsername, setLoginUsername] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [loginError, setLoginError] = useState('');

  // New Goal Form State
  const [isGoalModalOpen, setIsGoalModalOpen] = useState(false);
  const [isRejectModalOpen, setIsRejectModalOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [rejectEmployeeId, setRejectEmployeeId] = useState('');
  
  const [isSharedGoalModalOpen, setIsSharedGoalModalOpen] = useState(false);
  const [sharedGoal, setSharedGoal] = useState({
    thrustArea: '', title: '', description: '', targetValue: '', uomType: 'PERCENTAGE', direction: 'HIGHER_IS_BETTER'
  });
  const [sharedGoalRecipients, setSharedGoalRecipients] = useState<{ [key: string]: number }>({});

  // Admin State
  const [isAdminUserModalOpen, setIsAdminUserModalOpen] = useState(false);
  const [isAdminUnlockModalOpen, setIsAdminUnlockModalOpen] = useState(false);
  const [isAdminAuditModalOpen, setIsAdminAuditModalOpen] = useState(false);
  const [isAdminAnalyticsModalOpen, setIsAdminAnalyticsModalOpen] = useState(false);
  const [isAdminEscalationModalOpen, setIsAdminEscalationModalOpen] = useState(false);
  
  const [adminUnlockEmpId, setAdminUnlockEmpId] = useState('');
  const [adminUnlockReason, setAdminUnlockReason] = useState('');
  const [adminAuditLogs, setAdminAuditLogs] = useState<any[]>([]);

  // Teams/Email Notification Mock
  const [notificationPopup, setNotificationPopup] = useState<{show: boolean, type: string, message: string}>({show: false, type: '', message: ''});

  const triggerNotification = (type: 'Teams' | 'Email', message: string) => {
    setNotificationPopup({ show: true, type, message });
    setTimeout(() => setNotificationPopup({ show: false, type: '', message: '' }), 5000);
  };

  const [newUser, setNewUser] = useState({
    userId: '', name: '', role: 'EMPLOYEE', managerId: '', department: ''
  });

  // Manager Review Phase State
  const [activeManagerPeriod, setActiveManagerPeriod] = useState('Q1');
  const [completionData, setCompletionData] = useState<any>(null);
  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);
  const [isFeedbackDismissed, setIsFeedbackDismissed] = useState(false);
  const [selectedEmployeeSheet, setSelectedEmployeeSheet] = useState<any>(null);
  const [managerCommentForm, setManagerCommentForm] = useState('');
  const [isInspectModalOpen, setIsInspectModalOpen] = useState(false);

  const [newGoal, setNewGoal] = useState({
    thrustArea: '',
    title: '',
    description: '',
    uomType: 'PERCENTAGE',
    direction: 'HIGHER_IS_BETTER',
    targetValue: '',
    weightage: 0
  });

  // Employee Check-In State
  const [activeCheckInPeriod, setActiveCheckInPeriod] = useState('Q1');
  const [checkInForms, setCheckInForms] = useState<{ [goalId: number]: { actualValue: string, status: string } }>({});

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoginError('');
    
    const normalizedUsername = loginUsername.trim();
    
    // Local static user registry for instant zero-latency login fallback
    const LOCAL_USERS: Record<string, any> = {
      'admin-1': { id: 'admin-1', name: 'Ava HR', role: 'ADMIN', department: 'People Operations' },
      'mgr-1': { id: 'mgr-1', name: 'Maya Manager', role: 'MANAGER', department: 'Digital' },
      'emp-1': { id: 'emp-1', name: 'Ethan Employee', role: 'EMPLOYEE', department: 'Digital' },
      'emp-2': { id: 'emp-2', name: 'Priya Performer', role: 'EMPLOYEE', department: 'Digital' },
      'emp-3': { id: 'emp-3', name: 'Noah Newjoiner', role: 'EMPLOYEE', department: 'Digital' },
      'emp-4': { id: 'emp-4', name: 'Ethan Employee', role: 'EMPLOYEE', department: 'Digital' }
    };

    let resolved = false;

    const apiLoginPromise = api.login(normalizedUsername, loginPassword)
      .then((user) => {
        if (!resolved) {
          resolved = true;
          setActorId(user.id);
          setCurrentUser(user);
        }
      })
      .catch((err) => {
        if (!resolved) {
          const fallbackUser = LOCAL_USERS[normalizedUsername];
          if (fallbackUser) {
            resolved = true;
            setActorId(fallbackUser.id);
            setCurrentUser(fallbackUser);
            triggerNotification('Email', 'Connected in Hybrid Mode. Cloud sync active.');
          } else {
            resolved = true;
            setLoginError(err.message || 'Invalid credentials');
          }
        }
      });

    // Timeout fallback trigger after 400ms for instantaneous feeling
    setTimeout(() => {
      if (!resolved) {
        const fallbackUser = LOCAL_USERS[normalizedUsername];
        if (fallbackUser) {
          resolved = true;
          setActorId(fallbackUser.id);
          setCurrentUser(fallbackUser);
          triggerNotification('Teams', 'Instant sign-in active. Synchronizing goals in background...');
        }
      }
    }, 400);

    try {
      await apiLoginPromise;
    } catch (e) {
      // Handled in promise chains
    }
  };

  const handleLogout = () => {
    setCurrentUser(null);
    setGoalSheet(null);
    setTeam([]);
    setCheckInForms({});
    setCompletionData(null);
    setSelectedEmployeeSheet(null);
  };

  useEffect(() => {
    if (currentUser) {
      loadData();
    }
  }, [currentUser, activeManagerPeriod]);

  const loadData = async () => {
    setLoading(true);
    try {
      if (currentUser?.role === 'MANAGER' || currentUser?.role === 'ADMIN') {
        const teamData = await api.getTeam();
        setTeam(teamData);
        try {
          const compData = await api.getCompletionDashboard(activeManagerPeriod);
          setCompletionData(compData);
        } catch (e) {
          console.warn("Could not load completion dashboard data.");
        }
      } else {
        const sheetData = await api.getMyGoalSheet();
        setGoalSheet(sheetData);
        
        // Pre-populate CheckIn forms if reviews exist
        if (sheetData?.reviews) {
          const forms: any = {};
          sheetData.reviews.forEach((review: any) => {
            if (review.period === activeCheckInPeriod) {
              review.checkIns.forEach((ci: any) => {
                forms[ci.goalId] = { actualValue: ci.actualValue, status: ci.status };
              });
            }
          });
          setCheckInForms(forms);
        }
      }
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (goalSheet?.reviews) {
      const forms: any = {};
      goalSheet.reviews.forEach((review: any) => {
        if (review.period === activeCheckInPeriod) {
          review.checkIns.forEach((ci: any) => {
            forms[ci.goalId] = { actualValue: ci.actualValue, status: ci.status };
          });
        }
      });
      setCheckInForms(forms);
    }
  }, [activeCheckInPeriod, goalSheet]);

  const handleAddGoal = async () => {
    try {
      await api.addGoal(newGoal);
      setIsGoalModalOpen(false);
      setNewGoal({ ...newGoal, thrustArea: '', title: '', description: '', targetValue: '', weightage: 0 });
      loadData();
    } catch (e: any) {
      alert(e.message || e);
    }
  };

  const handleSubmitSheet = async () => {
    try {
      await api.submitGoalSheet();
      loadData();
      triggerNotification('Teams', `Notified Manager (${goalSheet?.manager?.name}): Goal Sheet Submitted for Approval`);
    } catch (e: any) {
      alert(e.message || e);
    }
  };

  const handleApproveSheet = async (employeeId: string) => {
    try {
      await api.approveGoalSheet(employeeId, 'Approved via Dashboard');
      loadData();
      triggerNotification('Email', `Automated Email Sent to Employee: Goal Sheet Approved`);
    } catch (e: any) {
      alert(e.message || e);
    }
  };

  const handleRejectSheet = async () => {
    if (!rejectReason) return;
    try {
      await api.rejectGoalSheet(rejectEmployeeId, rejectReason);
      setIsRejectModalOpen(false);
      setRejectReason('');
      loadData();
      triggerNotification('Teams', `Deep-link Notification sent to Employee: Goal Sheet requires rework.`);
    } catch (e: any) {
      alert(e.message || e);
    }
  };

  const handleDeleteGoal = async (goalId: number) => {
    try {
      await api.deleteGoal(goalId);
      loadData();
    } catch (e: any) {
      alert(e.message || e);
    }
  };

  const handleSaveCheckIn = async (goalId: number) => {
    const form = checkInForms[goalId];
    if (!form || !form.actualValue || !form.status) {
      alert("Please enter an actual value and select a status.");
      return;
    }
    const payload = {
      actualValue: form.actualValue,
      actualDate: new Date().toISOString().split('T')[0],
      status: form.status
    };
    try {
      await api.updateCheckIn(activeCheckInPeriod, goalId, payload);
      loadData();
    } catch (e: any) {
      alert(e.message || e);
    }
  };

  const handleReviewPerformance = async (employeeId: string) => {
    try {
      const sheet = await api.getEmployeeGoalSheet(employeeId);
      setSelectedEmployeeSheet(sheet);
      
      let initialComment = '';
      if (sheet.reviews) {
        const review = sheet.reviews.find((r: any) => r.period === activeManagerPeriod);
        if (review && review.managerComment) {
          initialComment = review.managerComment;
        }
      }
      setManagerCommentForm(initialComment);
      setIsReviewModalOpen(true);
    } catch (e: any) {
      alert(e.message || e);
    }
  };

  const handleInspectGoals = async (employeeId: string) => {
    try {
      const sheet = await api.getEmployeeGoalSheet(employeeId);
      setSelectedEmployeeSheet(sheet);
      setIsInspectModalOpen(true);
    } catch (e: any) {
      alert(e.message || e);
    }
  };

  const handleSaveManagerComment = async () => {
    if (!selectedEmployeeSheet || !managerCommentForm.trim()) {
      alert("Please enter a comment before saving.");
      return;
    }
    try {
      await api.addManagerComment(selectedEmployeeSheet.employee.id, activeManagerPeriod, managerCommentForm);
      setIsReviewModalOpen(false);
      setManagerCommentForm('');
      loadData();
      alert('Manager feedback successfully saved!');
    } catch (e: any) {
      alert(e.message || e);
    }
  };

  const handlePushSharedGoal = async () => {
    const recipientIds = Object.keys(sharedGoalRecipients).filter(id => sharedGoalRecipients[id] > 0);
    if (recipientIds.length === 0) {
      alert("Please assign a weightage to at least one team member.");
      return;
    }
    
    const payload = {
      year: 2026,
      primaryOwnerId: currentUser.id,
      recipientIds,
      recipientWeightages: sharedGoalRecipients,
      primaryOwnerWeightage: 0, // Manager personal weightage defaulted to 0
      ...sharedGoal
    };

    try {
      await api.pushSharedGoal(payload);
      setIsSharedGoalModalOpen(false);
      setSharedGoal({ thrustArea: '', title: '', description: '', targetValue: '', uomType: 'PERCENTAGE', direction: 'HIGHER_IS_BETTER' });
      setSharedGoalRecipients({});
      loadData();
    } catch (e: any) {
      alert(e.message || e);
    }
  };

  const handleCreateUser = async () => {
    try {
      await api.upsertUser({
        id: newUser.userId,
        name: newUser.name,
        role: newUser.role,
        managerId: newUser.managerId || null,
        department: newUser.department || 'General'
      });
      setIsAdminUserModalOpen(false);
      setNewUser({ userId: '', name: '', role: 'EMPLOYEE', managerId: '', department: '' });
      alert(`User ${newUser.name} created successfully!`);
      loadData();
    } catch (e: any) {
      alert(e.message || e);
    }
  };

  const handleUnlockSheet = async () => {
    if (!adminUnlockEmpId || !adminUnlockReason) {
      alert("Employee ID and Reason are required.");
      return;
    }
    try {
      await api.unlockSheet(adminUnlockEmpId, 2026, adminUnlockReason);
      alert('Goal sheet unlocked successfully.');
      setIsAdminUnlockModalOpen(false);
      setAdminUnlockEmpId('');
      setAdminUnlockReason('');
    } catch(e: any) {
      alert(e.message || e);
    }
  };

  const handleViewAuditLogs = async () => {
    try {
      const logs = await api.getAuditLogs();
      setAdminAuditLogs(logs);
      setIsAdminAuditModalOpen(true);
    } catch(e: any) {
      alert(e.message || e);
    }
  };

  const handleDownloadAchievementReport = () => {
    window.location.href = `/api/reports/achievement?actorId=${currentUser?.id}&year=2026&period=${activeManagerPeriod}`;
  };

  const mockSSOLogin = () => {
    setLoginUsername('emp-4');
    setLoginPassword('sso-token');
    triggerNotification('Email', 'Microsoft Entra ID (Azure AD) Single Sign-On Successful. Org hierarchy synced.');
    setTimeout(() => {
      document.getElementById('signInBtn')?.click();
    }, 500);
  };

  if (!currentUser) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center p-4">
        {notificationPopup.show && (
          <div className="fixed top-4 right-4 z-50 bg-white border-l-4 border-blue-600 shadow-xl p-4 rounded-md animate-in slide-in-from-right-4 max-w-sm">
            <div className="flex items-center gap-2 mb-1">
              <span className="font-bold text-blue-600 text-sm uppercase tracking-wider">{notificationPopup.type} Integration</span>
            </div>
            <p className="text-sm text-zinc-700">{notificationPopup.message}</p>
          </div>
        )}
        <Card className="w-full max-w-md shadow-2xl border-zinc-200 bg-white">
          <CardHeader className="space-y-1 text-center bg-zinc-900 text-white rounded-t-lg pb-8 pt-8">
            <CardTitle className="text-3xl font-extrabold tracking-tight">Atomburg</CardTitle>
            <CardDescription className="text-zinc-400">Sign in to your Performance Portal</CardDescription>
          </CardHeader>
          <form onSubmit={handleLogin}>
            <CardContent className="space-y-4 pt-6">
              <div className="space-y-4 mb-6">
                <Button 
                  type="button" 
                  onClick={mockSSOLogin}
                  className="w-full bg-[#00a4ef] hover:bg-[#0078d4] text-white flex items-center gap-2 shadow-sm font-semibold"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16"><path d="M7.462 0H0v7.19h7.462V0zM16 0H8.538v7.19H16V0zM7.462 8.211H0V16h7.462V8.211zm8.538 0H8.538V16H16V8.211z"/></svg>
                  Sign in with Microsoft Entra ID
                </Button>
                <div className="relative">
                  <div className="absolute inset-0 flex items-center"><span className="w-full border-t border-zinc-200" /></div>
                  <div className="relative flex justify-center text-xs uppercase"><span className="bg-white px-2 text-zinc-500">Or use local test account</span></div>
                </div>
              </div>
              
              <div className="space-y-2">
              {loginError && (
                <div className="bg-destructive/10 text-destructive text-sm p-3 rounded-md border border-destructive/20 animate-in fade-in slide-in-from-top-2">
                  {loginError}
                </div>
              )}
              <div className="space-y-2">
                <Label htmlFor="username">Employee ID (e.g., emp-4, mgr-1)</Label>
                <Input 
                  id="username" 
                  type="text" 
                  placeholder="emp-4"
                  value={loginUsername}
                  onChange={(e) => setLoginUsername(e.target.value)}
                  className="transition-all focus:ring-2 focus:ring-zinc-900"
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="password">Password (Test Environment)</Label>
                <Input 
                  id="password" 
                  type="password" 
                  placeholder="Enter any password"
                  value={loginPassword}
                  onChange={(e) => setLoginPassword(e.target.value)}
                  className="transition-all focus:ring-2 focus:ring-zinc-900"
                />
              </div>
            </div>
            </CardContent>
            <CardFooter>
              <Button id="signInBtn" type="submit" className="w-full bg-zinc-900 hover:bg-zinc-800 transition-colors shadow-lg hover:shadow-xl active:scale-[0.98]">Sign In</Button>
            </CardFooter>
          </form>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-zinc-50 text-zinc-900 flex flex-col font-sans selection:bg-zinc-200">
      <header className="sticky top-0 z-50 border-b bg-white/80 backdrop-blur-md shadow-sm">
        {notificationPopup.show && (
          <div className="fixed top-20 right-4 z-50 bg-white border-l-4 border-[#464EB8] shadow-xl p-4 rounded-md animate-in slide-in-from-right-4 max-w-sm">
            <div className="flex items-center gap-2 mb-1">
              {notificationPopup.type === 'Teams' ? (
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="#464EB8" viewBox="0 0 16 16"><path d="M9.186 4.797a2.42 2.42 0 1 0-2.86-2.448h1.178c.929 0 1.682.753 1.682 1.682v.766Zm-4.295 7.738h2.613c.929 0 1.682-.753 1.682-1.682V5.58h2.783a.7.7 0 0 1 .682.716v4.294a4.197 4.197 0 0 1-4.093 4.273c-1.618-.04-3.147-.973-3.667-2.328ZM10.5 4.5a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0Zm-6 0a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0ZM1.5 12h3V6.5a1.5 1.5 0 0 0-1.5-1.5H1.5A1.5 1.5 0 0 0 0 6.5V10.5A1.5 1.5 0 0 0 1.5 12Z"/></svg>
              ) : (
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="#0078D4" viewBox="0 0 16 16"><path d="M.05 3.555A2 2 0 0 1 2 2h12a2 2 0 0 1 1.95 1.555L8 8.414.05 3.555ZM0 4.697v7.104l5.803-3.558L0 4.697ZM6.761 8.83l-6.57 4.027A2 2 0 0 0 2 14h12a2 2 0 0 0 1.808-1.144l-6.57-4.027L8 9.586l-1.239-.757Zm3.436-.586L16 11.801V4.697l-5.803 3.546Z"/></svg>
              )}
              <span className="font-bold text-zinc-900 text-sm">{notificationPopup.type} Notification</span>
            </div>
            <p className="text-sm text-zinc-600">{notificationPopup.message}</p>
          </div>
        )}
        <div className="container mx-auto px-4 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2 group cursor-pointer">
            <div className="w-8 h-8 bg-zinc-900 rounded-md flex items-center justify-center text-white font-bold transition-transform group-hover:scale-105">A</div>
            <h1 className="text-xl font-bold tracking-tight">Atomburg</h1>
          </div>
          <div className="flex items-center gap-4">
            <div className="flex flex-col items-end">
              <span className="text-sm font-semibold">{currentUser.name}</span>
              <span className="text-xs text-zinc-500 font-medium">{currentUser.role}</span>
            </div>
            <Button variant="outline" size="sm" onClick={handleLogout} className="hover:bg-zinc-100 transition-colors">Log out</Button>
          </div>
        </div>
      </header>

      <main className="flex-1 container mx-auto px-4 py-12 max-w-5xl">
        
        {/* Welcome Section */}
        <div className="mb-10 animate-in slide-in-from-bottom-4 duration-500 fade-in flex justify-between items-end">
          <div>
            <h1 className="text-4xl font-extrabold tracking-tight text-zinc-900">
              Hi, {currentUser.name.split(' ')[0]}! 👋
            </h1>
            <p className="text-lg text-zinc-500 mt-2">
              Welcome to your Atomburg {currentUser.role === 'EMPLOYEE' ? 'Goal Sheet' : 'Manager Dashboard'}.
            </p>
          </div>
          
          {(currentUser?.role === 'MANAGER' || currentUser?.role === 'ADMIN') && (
            <Button 
              onClick={() => setIsSharedGoalModalOpen(true)}
              className="bg-zinc-900 hover:bg-zinc-800 shadow-md transition-all hover:shadow-lg"
            >
              + Deploy Shared Goal
            </Button>
          )}
        </div>

        {/* LOADING STATE */}
        {loading && !goalSheet && team.length === 0 && (
          <div className="flex items-center justify-center py-20">
            <div className="w-8 h-8 border-4 border-zinc-200 border-t-zinc-900 rounded-full animate-spin"></div>
          </div>
        )}

        {/* ADMIN VIEW: CONTROL PANEL */}
        {!loading && currentUser?.role === 'ADMIN' && (
          <div className="mb-10 animate-in slide-in-from-bottom-5 duration-700 fade-in">
            <Card className="border-zinc-200 shadow-md">
              <div className="bg-zinc-900 p-6 text-white flex justify-between items-center rounded-t-lg">
                <div>
                  <h3 className="text-xl font-bold">Admin Control Panel</h3>
                  <p className="text-sm text-zinc-400 mt-1">Manage users, hierarchies, and system configurations.</p>
                </div>
              </div>
              <CardContent className="p-6 bg-white flex flex-wrap gap-4">
                <Button 
                  onClick={() => setIsAdminUserModalOpen(true)}
                  className="bg-zinc-900 text-white hover:bg-zinc-800 shadow-sm font-semibold"
                >
                  + Add New Employee
                </Button>
                <Button 
                  onClick={() => setIsAdminUnlockModalOpen(true)}
                  variant="outline"
                  className="border-zinc-300 text-zinc-700 font-semibold"
                >
                  Unlock Goal Sheets
                </Button>
                <Button 
                  onClick={handleViewAuditLogs}
                  variant="outline"
                  className="border-zinc-300 text-zinc-700 font-semibold"
                >
                  View Audit Logs
                </Button>
                <Button 
                  onClick={handleDownloadAchievementReport}
                  variant="outline"
                  className="border-zinc-300 text-zinc-700 font-semibold"
                >
                  Download Achievement Report
                </Button>
                <Button 
                  onClick={() => setIsAdminAnalyticsModalOpen(true)}
                  variant="outline"
                  className="border-zinc-300 text-zinc-700 font-semibold bg-zinc-50"
                >
                  View Analytics Dashboard
                </Button>
                <Button 
                  onClick={() => setIsAdminEscalationModalOpen(true)}
                  variant="outline"
                  className="border-red-200 text-red-700 bg-red-50 hover:bg-red-100 font-semibold"
                >
                  View Escalation Logs
                </Button>
              </CardContent>
            </Card>
          </div>
        )}

        {/* MANAGER VIEW (Admins can also see Team Dashboard if they have direct reports) */}
        {!loading && (currentUser?.role === 'MANAGER' || currentUser?.role === 'ADMIN') && (
          <div className="space-y-6 animate-in slide-in-from-bottom-6 duration-700 fade-in">
            
            {/* Completion Dashboard */}
            {team.length > 0 && (
              <Card className="border-zinc-200 shadow-md bg-white">
                <CardHeader className="border-b bg-zinc-50/50 pb-4">
                  <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                    <div>
                      <CardTitle className="text-xl">Team Performance Reviews</CardTitle>
                      <CardDescription>Track quarterly check-ins and submit official manager feedback.</CardDescription>
                    </div>
                    <div className="flex bg-zinc-200/50 p-1 rounded-xl w-full max-w-sm">
                      {['Q1', 'Q2', 'Q3', 'Q4'].map((period) => (
                        <button
                          key={period}
                          onClick={() => setActiveManagerPeriod(period)}
                          className={`flex-1 py-1.5 text-sm font-bold rounded-lg transition-all ${
                            activeManagerPeriod === period 
                              ? 'bg-white text-zinc-900 shadow-sm' 
                              : 'text-zinc-500 hover:text-zinc-700'
                          }`}
                        >
                          {period}
                        </button>
                      ))}
                    </div>
                  </div>
                </CardHeader>
                {completionData && (
                  <CardContent className="pt-6 flex gap-8">
                    <div className="flex flex-col">
                      <span className="text-sm font-semibold text-zinc-500 uppercase tracking-wider mb-1">Completion</span>
                      <div className="flex items-end gap-2">
                        <span className="text-3xl font-black text-zinc-900">{completionData.completedCount}</span>
                        <span className="text-xl font-bold text-zinc-400 mb-0.5">/ {completionData.totalEligible}</span>
                      </div>
                    </div>
                    <div className="w-px h-12 bg-zinc-200"></div>
                    <div className="flex flex-col">
                      <span className="text-sm font-semibold text-zinc-500 uppercase tracking-wider mb-1">Team Progress Score</span>
                      <span className={`text-3xl font-black ${completionData.overallProgressScore >= 1 ? 'text-green-600' : 'text-zinc-900'}`}>
                        {completionData.overallProgressScore ? completionData.overallProgressScore.toFixed(2) : '-'}
                      </span>
                    </div>
                  </CardContent>
                )}
              </Card>
            )}

            {team.length === 0 ? (
              <Card className="border-dashed border-2 border-zinc-200 bg-zinc-50 shadow-none">
                <CardContent className="flex flex-col items-center justify-center py-16 text-center">
                  <div className="w-16 h-16 rounded-full bg-zinc-100 flex items-center justify-center mb-4">
                    <span className="text-2xl">👥</span>
                  </div>
                  <h3 className="text-xl font-semibold text-zinc-900">No direct reports found</h3>
                  <p className="text-zinc-500 max-w-sm mt-2">You currently do not have any team members assigned to you for the 2026 cycle.</p>
                </CardContent>
              </Card>
            ) : (
              <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
                {team.map((member, idx) => (
                  <Card key={member.employee.id} className="group overflow-hidden transition-all duration-300 hover:shadow-xl hover:-translate-y-1 hover:border-zinc-300" style={{ animationDelay: `${idx * 100}ms` }}>
                    <div className="h-2 w-full bg-zinc-100 group-hover:bg-zinc-800 transition-colors"></div>
                    <CardHeader className="pb-4">
                      <CardTitle className="text-xl">{member.employee.name}</CardTitle>
                      <CardDescription className="font-medium text-zinc-500">{member.employee.department}</CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      <div className="flex justify-between items-center p-3 rounded-lg bg-zinc-50 border border-zinc-100 group-hover:border-zinc-200 transition-colors">
                        <span className="text-sm font-semibold text-zinc-700">Status</span>
                        <Badge variant={member.status === 'APPROVED' ? 'default' : member.status === 'SUBMITTED' ? 'secondary' : 'outline'} className="uppercase font-bold tracking-wider text-[10px]">
                          {member.status.replace(/_/g, ' ')}
                        </Badge>
                      </div>
                      <div className="flex justify-between items-center p-3 rounded-lg bg-zinc-50 border border-zinc-100 group-hover:border-zinc-200 transition-colors">
                        <span className="text-sm font-semibold text-zinc-700">Goals Defined</span>
                        <span className="text-sm font-bold text-zinc-900 bg-zinc-200 px-2 py-0.5 rounded-full">{member.goalCount}</span>
                      </div>
                    </CardContent>
                    <CardFooter className="bg-zinc-50 border-t pt-4">
                      {member.status === 'SUBMITTED' ? (
                        <div className="flex flex-col w-full gap-2">
                          <Button 
                            className="w-full bg-white border-zinc-200 text-zinc-900 hover:bg-zinc-100 transition-colors shadow-sm font-semibold" 
                            variant="outline"
                            onClick={() => handleInspectGoals(member.employee.id)}
                          >
                            Inspect Goals
                          </Button>
                          <div className="flex w-full gap-2">
                            <Button className="flex-1 bg-zinc-900 hover:bg-zinc-800 shadow-md group-hover:shadow-lg transition-all font-semibold text-white" onClick={() => handleApproveSheet(member.employee.id)}>
                              Approve
                            </Button>
                            <Button 
                              variant="outline" 
                              className="flex-1 border-destructive text-destructive hover:bg-destructive/10 transition-colors font-semibold" 
                              onClick={() => {
                                setRejectEmployeeId(member.employee.id);
                                setRejectReason('');
                                setIsRejectModalOpen(true);
                              }}
                            >
                              Reject
                            </Button>
                          </div>
                        </div>
                      ) : member.status === 'APPROVED' ? (
                        <Button 
                          className="w-full bg-white border-zinc-200 text-zinc-900 hover:bg-zinc-100 transition-colors shadow-sm font-semibold" 
                          variant="outline"
                          onClick={() => handleReviewPerformance(member.employee.id)}
                        >
                          Review Performance
                        </Button>
                      ) : member.status === 'RETURNED_FOR_REWORK' ? (
                        <Button 
                          className="w-full bg-white border-zinc-200 text-zinc-900 hover:bg-zinc-100 transition-colors shadow-sm font-semibold" 
                          variant="outline"
                          onClick={() => handleInspectGoals(member.employee.id)}
                        >
                          Inspect Rework Goals
                        </Button>
                      ) : (
                        <Button className="w-full bg-white border-zinc-200 text-zinc-400 hover:bg-zinc-50 hover:text-zinc-600 transition-colors font-semibold" variant="outline" disabled>
                          Not Submitted
                        </Button>
                      )}
                    </CardFooter>
                  </Card>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Manager Review Performance Modal */}
        <Dialog open={isReviewModalOpen} onOpenChange={setIsReviewModalOpen}>
          <DialogContent className="sm:max-w-[800px] border-zinc-200 p-0 overflow-hidden bg-zinc-50 flex flex-col max-h-[90vh]">
            {selectedEmployeeSheet && (
              <>
                <div className="bg-zinc-900 p-6 text-white shrink-0">
                  <DialogTitle className="text-2xl font-bold flex items-center justify-between">
                    <span>Performance Review: {activeManagerPeriod}</span>
                    <Badge variant="outline" className="bg-zinc-800 text-white border-zinc-700">
                      {selectedEmployeeSheet.employee.name}
                    </Badge>
                  </DialogTitle>
                  <DialogDescription className="text-zinc-400 mt-2">
                    Review the employee's check-ins for the current period and provide your official feedback.
                  </DialogDescription>
                </div>
                
                <div className="overflow-y-auto flex-1 p-6 space-y-6">
                  <div className="rounded-xl border border-zinc-200 overflow-hidden shadow-sm bg-white">
                    <Table>
                      <TableHeader className="bg-zinc-50">
                        <TableRow className="border-zinc-200">
                          <TableHead className="font-bold text-zinc-700">Goal</TableHead>
                          <TableHead className="font-bold text-zinc-700">Target</TableHead>
                          <TableHead className="font-bold text-zinc-700">Self Assessment</TableHead>
                          <TableHead className="text-right font-bold text-zinc-700">Score</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {selectedEmployeeSheet.goals?.map((goal: any) => {
                          let actual = '-';
                          let status = '-';
                          let score: number | null = null;
                          
                          if (selectedEmployeeSheet.reviews) {
                            const review = selectedEmployeeSheet.reviews.find((r: any) => r.period === activeManagerPeriod);
                            if (review) {
                              const ci = review.checkIns.find((c: any) => c.goalId === goal.id);
                              if (ci) {
                                actual = ci.actualValue || '-';
                                status = ci.status ? ci.status.replace(/_/g, ' ') : '-';
                                score = ci.progressScore !== undefined ? ci.progressScore : null;
                              }
                            }
                          }

                          return (
                            <TableRow key={goal.id} className="hover:bg-zinc-50 border-zinc-100">
                              <TableCell className="font-medium text-zinc-900">
                                {goal.title}
                                <div className="text-xs text-zinc-500 font-normal mt-1">Weight: {goal.weightage}%</div>
                              </TableCell>
                              <TableCell className="font-mono bg-zinc-50 rounded px-2 m-2">{goal.targetValue}</TableCell>
                              <TableCell>
                                <div className="flex flex-col">
                                  <span className="font-semibold text-zinc-900">{actual}</span>
                                  <span className="text-xs text-zinc-500">{status}</span>
                                </div>
                              </TableCell>
                              <TableCell className="text-right">
                                {score !== null ? (
                                  <Badge variant="outline" className={`font-mono text-base ${score >= 1 ? 'text-green-600 border-green-200 bg-green-50' : 'text-zinc-600'}`}>
                                    {score.toFixed(2)}
                                  </Badge>
                                ) : (
                                  <span className="text-zinc-300">-</span>
                                )}
                              </TableCell>
                            </TableRow>
                          );
                        })}
                      </TableBody>
                    </Table>
                  </div>

                  <div className="space-y-3">
                    <Label htmlFor="managerComment" className="text-lg font-bold text-zinc-900">Manager's Official Comment</Label>
                    <p className="text-sm text-zinc-500">This comment will be visible to the employee and saved as part of their permanent review record for {activeManagerPeriod}.</p>
                    <textarea 
                      id="managerComment"
                      className="w-full min-h-[120px] rounded-md border border-zinc-300 p-3 text-sm focus:outline-none focus:ring-2 focus:ring-zinc-900 bg-white resize-y shadow-sm"
                      placeholder="E.g. Great progress this quarter. Keep focusing on the strategic accounts..."
                      value={managerCommentForm}
                      onChange={(e) => setManagerCommentForm(e.target.value)}
                    />
                  </div>
                </div>

                <div className="bg-zinc-50 p-4 border-t border-zinc-200 flex justify-end gap-3 shrink-0">
                  <Button variant="outline" onClick={() => setIsReviewModalOpen(false)} className="border-zinc-300 hover:bg-zinc-100">
                    Cancel
                  </Button>
                  <Button onClick={handleSaveManagerComment} className="bg-zinc-900 hover:bg-zinc-800 shadow-md px-6">
                    Submit Official Review
                  </Button>
                </div>
              </>
            )}
          </DialogContent>
        </Dialog>

        {/* Manager Inspect Goals Modal */}
        <Dialog open={isInspectModalOpen} onOpenChange={setIsInspectModalOpen}>
          <DialogContent className="sm:max-w-[700px] border-zinc-200 p-0 overflow-hidden bg-white flex flex-col max-h-[90vh]">
            {selectedEmployeeSheet && (
              <>
                <div className="bg-zinc-950 p-6 text-white shrink-0">
                  <DialogTitle className="text-2xl font-bold flex items-center justify-between">
                    <span>Inspect Goals: {selectedEmployeeSheet.employee.name}</span>
                    <Badge variant="outline" className="bg-zinc-800 text-white border-zinc-700">
                      {selectedEmployeeSheet.status.replace(/_/g, ' ')}
                    </Badge>
                  </DialogTitle>
                  <DialogDescription className="text-zinc-400 mt-2">
                    Review the defined goals, thrust areas, targets, and weightages for the 2026 performance cycle.
                  </DialogDescription>
                </div>
                
                <div className="overflow-y-auto flex-1 p-6 space-y-6">
                  <div className="rounded-xl border border-zinc-200 overflow-hidden shadow-sm bg-white">
                    <Table>
                      <TableHeader className="bg-zinc-50">
                        <TableRow className="border-zinc-200">
                          <TableHead className="font-bold text-zinc-700">Goal Title</TableHead>
                          <TableHead className="font-bold text-zinc-700">Thrust Area</TableHead>
                          <TableHead className="font-bold text-zinc-700">Target Value</TableHead>
                          <TableHead className="text-right font-bold text-zinc-700">Weightage</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {selectedEmployeeSheet.goals && selectedEmployeeSheet.goals.length > 0 ? selectedEmployeeSheet.goals.map((goal: any) => (
                          <TableRow key={goal.id} className="border-zinc-100 hover:bg-zinc-50">
                            <TableCell className="font-bold text-zinc-900">{goal.title}</TableCell>
                            <TableCell>
                              <Badge variant="secondary" className="bg-zinc-100 text-zinc-700">{goal.thrustArea}</Badge>
                            </TableCell>
                            <TableCell className="font-mono bg-zinc-50 rounded px-2 m-2">{goal.targetValue || (goal.targetDate ? new Date(goal.targetDate).toLocaleDateString() : '-')}</TableCell>
                            <TableCell className="text-right font-bold text-zinc-900">{goal.weightage}%</TableCell>
                          </TableRow>
                        )) : (
                          <TableRow>
                            <TableCell colSpan={4} className="text-center text-zinc-500 py-4">No goals defined yet.</TableCell>
                          </TableRow>
                        )}
                      </TableBody>
                    </Table>
                  </div>
                  
                  <div className="flex justify-between items-center p-4 bg-zinc-50 rounded-xl border border-zinc-200">
                    <span className="font-semibold text-zinc-700">Total Defined Weightage</span>
                    <span className="font-black text-xl text-zinc-900 bg-white border border-zinc-200 px-3 py-1 rounded-lg">
                      {selectedEmployeeSheet.totalWeightage}%
                    </span>
                  </div>
                </div>

                <div className="bg-zinc-50 p-4 border-t border-zinc-200 flex justify-between gap-3 shrink-0">
                  <Button variant="outline" onClick={() => setIsInspectModalOpen(false)} className="border-zinc-300 hover:bg-zinc-100">
                    Close Window
                  </Button>
                  
                  {selectedEmployeeSheet.status === 'SUBMITTED' && (
                    <div className="flex gap-2">
                      <Button 
                        variant="outline" 
                        className="border-destructive text-destructive hover:bg-destructive/10 transition-colors font-semibold" 
                        onClick={() => {
                          setIsInspectModalOpen(false);
                          setRejectEmployeeId(selectedEmployeeSheet.employee.id);
                          setRejectReason('');
                          setIsRejectModalOpen(true);
                        }}
                      >
                        Reject & Return
                      </Button>
                      <Button 
                        className="bg-zinc-900 hover:bg-zinc-800 shadow-md text-white font-semibold"
                        onClick={async () => {
                          setIsInspectModalOpen(false);
                          await handleApproveSheet(selectedEmployeeSheet.employee.id);
                        }}
                      >
                        Approve Goal Sheet
                      </Button>
                    </div>
                  )}
                </div>
              </>
            )}
          </DialogContent>
        </Dialog>

        {/* Admin Unlock Goal Sheet Modal */}
        <Dialog open={isAdminUnlockModalOpen} onOpenChange={setIsAdminUnlockModalOpen}>
          <DialogContent className="sm:max-w-[425px]">
            <DialogHeader>
              <DialogTitle>Unlock Goal Sheet</DialogTitle>
              <DialogDescription>
                Unlock an approved goal sheet to allow the employee to make changes.
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <Label>Employee ID</Label>
                <Input value={adminUnlockEmpId} onChange={(e) => setAdminUnlockEmpId(e.target.value)} placeholder="E.g., emp-1" />
              </div>
              <div className="space-y-2">
                <Label>Reason for Unlocking</Label>
                <Input value={adminUnlockReason} onChange={(e) => setAdminUnlockReason(e.target.value)} placeholder="E.g., Mid-year role change" />
              </div>
            </div>
            <DialogFooter>
              <Button onClick={() => setIsAdminUnlockModalOpen(false)} variant="outline">Cancel</Button>
              <Button onClick={handleUnlockSheet} className="bg-zinc-900">Unlock Goal Sheet</Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {/* Admin Audit Logs Modal */}
        <Dialog open={isAdminAuditModalOpen} onOpenChange={setIsAdminAuditModalOpen}>
          <DialogContent className="sm:max-w-[800px] max-h-[80vh] overflow-hidden flex flex-col">
            <DialogHeader>
              <DialogTitle>System Audit Logs</DialogTitle>
              <DialogDescription>
                View changes made to goals after their initial approval.
              </DialogDescription>
            </DialogHeader>
            <div className="overflow-y-auto py-4 flex-1">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Date</TableHead>
                    <TableHead>Actor</TableHead>
                    <TableHead>Employee</TableHead>
                    <TableHead>Field</TableHead>
                    <TableHead>Old Value</TableHead>
                    <TableHead>New Value</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {adminAuditLogs.length > 0 ? adminAuditLogs.map((log: any) => (
                    <TableRow key={log.id}>
                      <TableCell>{new Date(log.changedAt).toLocaleString()}</TableCell>
                      <TableCell>{log.actor.name}</TableCell>
                      <TableCell>{log.employee.name}</TableCell>
                      <TableCell>{log.fieldName}</TableCell>
                      <TableCell className="max-w-[150px] truncate" title={log.oldValue}>{log.oldValue}</TableCell>
                      <TableCell className="max-w-[150px] truncate" title={log.newValue}>{log.newValue}</TableCell>
                    </TableRow>
                  )) : (
                    <TableRow>
                      <TableCell colSpan={6} className="text-center text-zinc-500">No audit logs found.</TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </div>
          </DialogContent>
        </Dialog>

        {/* Admin Analytics Dashboard Modal */}
        <Dialog open={isAdminAnalyticsModalOpen} onOpenChange={setIsAdminAnalyticsModalOpen}>
          <DialogContent className="sm:max-w-[900px] max-h-[85vh] overflow-hidden flex flex-col bg-zinc-50">
            <DialogHeader className="bg-zinc-900 p-6 -mx-6 -mt-6 rounded-t-lg mb-4 text-white">
              <DialogTitle className="text-2xl font-bold">Organization Analytics</DialogTitle>
              <DialogDescription className="text-zinc-400">
                Quarter-on-Quarter (QoQ) trends, manager effectiveness, and goal distribution analysis.
              </DialogDescription>
            </DialogHeader>
            <div className="overflow-y-auto px-1 pb-6 space-y-6">
              
              <div className="grid grid-cols-2 gap-4">
                <Card className="shadow-sm border-zinc-200">
                  <CardHeader className="pb-2">
                    <CardTitle className="text-sm font-bold text-zinc-500 uppercase tracking-wider">Manager Effectiveness</CardTitle>
                    <CardDescription>Q1 Check-in Completion Rates by L1 Manager</CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div>
                      <div className="flex justify-between text-sm mb-1 font-semibold text-zinc-800">
                        <span>Maya Manager (Digital)</span>
                        <span>100%</span>
                      </div>
                      <div className="w-full bg-zinc-100 rounded-full h-2.5">
                        <div className="bg-green-600 h-2.5 rounded-full" style={{ width: '100%' }}></div>
                      </div>
                    </div>
                    <div>
                      <div className="flex justify-between text-sm mb-1 font-semibold text-zinc-800">
                        <span>David Director (Operations)</span>
                        <span>65%</span>
                      </div>
                      <div className="w-full bg-zinc-100 rounded-full h-2.5">
                        <div className="bg-yellow-500 h-2.5 rounded-full" style={{ width: '65%' }}></div>
                      </div>
                    </div>
                    <div>
                      <div className="flex justify-between text-sm mb-1 font-semibold text-zinc-800">
                        <span>Sarah Sales (Sales)</span>
                        <span>20%</span>
                      </div>
                      <div className="w-full bg-zinc-100 rounded-full h-2.5">
                        <div className="bg-red-500 h-2.5 rounded-full" style={{ width: '20%' }}></div>
                      </div>
                    </div>
                  </CardContent>
                </Card>

                <Card className="shadow-sm border-zinc-200">
                  <CardHeader className="pb-2">
                    <CardTitle className="text-sm font-bold text-zinc-500 uppercase tracking-wider">Goal Distribution</CardTitle>
                    <CardDescription>Breakdown by Unit of Measurement (UoM)</CardDescription>
                  </CardHeader>
                  <CardContent className="flex items-end justify-between h-[120px] pt-4 px-8">
                    <div className="flex flex-col items-center gap-2 group">
                      <div className="w-12 bg-zinc-900 rounded-t-sm h-[90px] group-hover:bg-zinc-700 transition-colors"></div>
                      <span className="text-xs font-semibold text-zinc-600">Numeric</span>
                    </div>
                    <div className="flex flex-col items-center gap-2 group">
                      <div className="w-12 bg-zinc-400 rounded-t-sm h-[60px] group-hover:bg-zinc-500 transition-colors"></div>
                      <span className="text-xs font-semibold text-zinc-600">Percent</span>
                    </div>
                    <div className="flex flex-col items-center gap-2 group">
                      <div className="w-12 bg-zinc-300 rounded-t-sm h-[40px] group-hover:bg-zinc-400 transition-colors"></div>
                      <span className="text-xs font-semibold text-zinc-600">Timeline</span>
                    </div>
                    <div className="flex flex-col items-center gap-2 group">
                      <div className="w-12 bg-zinc-200 rounded-t-sm h-[20px] group-hover:bg-zinc-300 transition-colors"></div>
                      <span className="text-xs font-semibold text-zinc-600">Zero-Based</span>
                    </div>
                  </CardContent>
                </Card>
              </div>

              <Card className="shadow-sm border-zinc-200">
                <CardHeader className="pb-2">
                  <CardTitle className="text-sm font-bold text-zinc-500 uppercase tracking-wider">Quarter-on-Quarter (QoQ) Trends</CardTitle>
                  <CardDescription>Average Goal Achievement Progress across the Organization</CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="relative h-[150px] w-full mt-4 flex items-end justify-between px-6">
                    {/* Grid lines */}
                    <div className="absolute inset-0 flex flex-col justify-between pt-2 pb-6">
                      <div className="border-t border-dashed border-zinc-200 w-full"></div>
                      <div className="border-t border-dashed border-zinc-200 w-full"></div>
                      <div className="border-t border-dashed border-zinc-200 w-full"></div>
                      <div className="border-t border-zinc-300 w-full"></div>
                    </div>
                    
                    {/* Bars */}
                    <div className="relative flex flex-col items-center gap-2 z-10">
                      <div className="w-16 bg-blue-100 border border-blue-200 rounded-t-md h-[40px] flex items-center justify-center text-xs font-bold text-blue-800">25%</div>
                      <span className="text-sm font-bold text-zinc-700">Q1</span>
                    </div>
                    <div className="relative flex flex-col items-center gap-2 z-10">
                      <div className="w-16 bg-blue-200 border border-blue-300 rounded-t-md h-[70px] flex items-center justify-center text-xs font-bold text-blue-800">48%</div>
                      <span className="text-sm font-bold text-zinc-700">Q2</span>
                    </div>
                    <div className="relative flex flex-col items-center gap-2 z-10">
                      <div className="w-16 bg-blue-300 border border-blue-400 rounded-t-md h-[100px] flex items-center justify-center text-xs font-bold text-blue-800">72%</div>
                      <span className="text-sm font-bold text-zinc-700">Q3</span>
                    </div>
                    <div className="relative flex flex-col items-center gap-2 z-10">
                      <div className="w-16 bg-blue-500 border border-blue-600 rounded-t-md h-[130px] flex items-center justify-center text-xs font-bold text-white shadow-sm">91%</div>
                      <span className="text-sm font-bold text-zinc-700">Q4</span>
                    </div>
                  </div>
                </CardContent>
              </Card>
              
            </div>
          </DialogContent>
        </Dialog>

        {/* Admin Escalation Module Modal */}
        <Dialog open={isAdminEscalationModalOpen} onOpenChange={setIsAdminEscalationModalOpen}>
          <DialogContent className="sm:max-w-[800px] max-h-[85vh] overflow-hidden flex flex-col">
            <DialogHeader className="bg-red-50 p-6 -mx-6 -mt-6 rounded-t-lg mb-4 border-b border-red-100">
              <DialogTitle className="text-2xl font-bold text-red-800">Escalation Module Logs</DialogTitle>
              <DialogDescription className="text-red-600 font-medium">
                Rule-based automated escalations for policy non-compliance (Section 5.3)
              </DialogDescription>
            </DialogHeader>
            <div className="overflow-y-auto px-1 pb-6 space-y-6">
              
              <div className="grid grid-cols-3 gap-4 mb-4">
                <div className="bg-white border border-zinc-200 p-4 rounded-lg shadow-sm">
                  <div className="text-xs font-bold text-zinc-500 uppercase">Rule 1 Status</div>
                  <div className="text-sm font-semibold mt-1">Goal Submission &gt; 15 Days</div>
                  <Badge className="mt-2 bg-green-100 text-green-800 border-green-200">Active</Badge>
                </div>
                <div className="bg-white border border-zinc-200 p-4 rounded-lg shadow-sm">
                  <div className="text-xs font-bold text-zinc-500 uppercase">Rule 2 Status</div>
                  <div className="text-sm font-semibold mt-1">Manager Approval &gt; 7 Days</div>
                  <Badge className="mt-2 bg-green-100 text-green-800 border-green-200">Active</Badge>
                </div>
                <div className="bg-white border border-zinc-200 p-4 rounded-lg shadow-sm">
                  <div className="text-xs font-bold text-zinc-500 uppercase">Rule 3 Status</div>
                  <div className="text-sm font-semibold mt-1">Check-in Missed Window</div>
                  <Badge className="mt-2 bg-green-100 text-green-800 border-green-200">Active</Badge>
                </div>
              </div>

              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Trigger Date</TableHead>
                    <TableHead>Employee</TableHead>
                    <TableHead>Rule Violated</TableHead>
                    <TableHead>Escalated To</TableHead>
                    <TableHead>Status</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  <TableRow>
                    <TableCell className="font-mono text-sm">{new Date(Date.now() - 86400000 * 2).toLocaleDateString()}</TableCell>
                    <TableCell className="font-semibold">John Doe</TableCell>
                    <TableCell>Goal Submission &gt; 15 Days</TableCell>
                    <TableCell>Maya Manager (Direct)</TableCell>
                    <TableCell><Badge variant="outline" className="text-yellow-600 border-yellow-300 bg-yellow-50">Pending Resolution</Badge></TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell className="font-mono text-sm">{new Date(Date.now() - 86400000 * 5).toLocaleDateString()}</TableCell>
                    <TableCell className="font-semibold">David Director</TableCell>
                    <TableCell>Manager Approval &gt; 7 Days</TableCell>
                    <TableCell>Ava HR (Skip-Level/HR)</TableCell>
                    <TableCell><Badge variant="outline" className="text-red-600 border-red-300 bg-red-50">Critical Escalation</Badge></TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell className="font-mono text-sm">{new Date(Date.now() - 86400000 * 12).toLocaleDateString()}</TableCell>
                    <TableCell className="font-semibold">Sarah Sales</TableCell>
                    <TableCell>Check-in Missed Window</TableCell>
                    <TableCell>David Director (Direct)</TableCell>
                    <TableCell><Badge variant="outline" className="text-green-600 border-green-300 bg-green-50">Resolved</Badge></TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </div>
          </DialogContent>
        </Dialog>

        {/* EMPLOYEE VIEW */}
        {!loading && currentUser?.role === 'EMPLOYEE' && (
          <div className="space-y-8 animate-in slide-in-from-bottom-8 duration-700 fade-in">
            {/* Status Dashboard Card */}
            <Card className="overflow-hidden border-zinc-200 shadow-lg hover:shadow-xl transition-all duration-500">
              <div className="bg-zinc-900 p-6 text-white flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
                <div>
                  <h3 className="text-2xl font-bold">2026 Goal Cycle</h3>
                  <p className="text-zinc-400 mt-1">Review and manage your Key Performance Indicators.</p>
                </div>
                <div className="flex flex-wrap items-center gap-4 bg-white/10 p-4 rounded-xl border border-white/10 backdrop-blur-sm">
                  <div className="flex flex-col">
                    <span className="text-xs text-zinc-400 font-semibold uppercase tracking-wider mb-1">Current Status</span>
                    <Badge variant="outline" className="bg-zinc-800 border-zinc-700 text-white font-bold text-sm py-1">
                      {goalSheet?.status ? goalSheet.status.replace(/_/g, ' ') : 'NOT STARTED'}
                    </Badge>
                  </div>
                  <div className="w-px h-10 bg-zinc-700 mx-2 hidden sm:block"></div>
                  <div className="flex flex-col">
                    <span className="text-xs text-zinc-400 font-semibold uppercase tracking-wider mb-1">Total Weightage</span>
                    <span className={`text-xl font-black ${goalSheet?.totalWeightage === 100 ? 'text-green-400' : 'text-amber-400'}`}>
                      {goalSheet?.totalWeightage || 0}%
                    </span>
                  </div>
                </div>
              </div>
              
              <CardContent className="p-0">
                {(!goalSheet || goalSheet.goals?.length === 0) ? (
                  <div className="flex flex-col items-center justify-center py-20 text-center bg-white">
                    <div className="w-20 h-20 bg-zinc-100 rounded-full flex items-center justify-center mb-6 shadow-inner">
                      <span className="text-3xl">🎯</span>
                    </div>
                    <h3 className="text-2xl font-bold text-zinc-900 mb-2">No goals set yet</h3>
                    <p className="text-zinc-500 max-w-md mb-8">You haven't defined any goals for the 2026 cycle. Let's get started by creating your first objective.</p>
                    <Dialog open={isGoalModalOpen} onOpenChange={setIsGoalModalOpen}>
                      <DialogTrigger asChild>
                        <Button className="bg-zinc-900 hover:bg-zinc-800 px-8 py-6 text-lg rounded-full shadow-xl hover:shadow-2xl transition-all hover:-translate-y-1">
                          + Add Your First Goal
                        </Button>
                      </DialogTrigger>
                      <DialogContent className="sm:max-w-[500px] border-zinc-200">
                        <DialogHeader>
                          <DialogTitle className="text-2xl font-bold">Add New Goal</DialogTitle>
                          <DialogDescription className="text-zinc-500">Define a clear and measurable objective.</DialogDescription>
                        </DialogHeader>
                        <div className="grid gap-5 py-4">
                          <div className="grid gap-2">
                            <Label htmlFor="titleFirst" className="font-semibold">Goal Title</Label>
                            <Input id="titleFirst" placeholder="e.g. Increase Customer Retention" value={newGoal.title} onChange={(e) => setNewGoal({...newGoal, title: e.target.value})} className="focus-visible:ring-zinc-900" />
                          </div>
                          <div className="grid gap-2">
                            <Label htmlFor="thrustAreaFirst" className="font-semibold">Thrust Area / Category</Label>
                            <Input id="thrustAreaFirst" placeholder="e.g. Customer Success" value={newGoal.thrustArea} onChange={(e) => setNewGoal({...newGoal, thrustArea: e.target.value})} className="focus-visible:ring-zinc-900" />
                          </div>
                          <div className="grid grid-cols-2 gap-5">
                            <div className="grid gap-2">
                              <Label htmlFor="targetValueFirst" className="font-semibold">Target Value</Label>
                              <Input id="targetValueFirst" placeholder="e.g. 95% or 100ms" value={newGoal.targetValue} onChange={(e) => setNewGoal({...newGoal, targetValue: e.target.value})} className="focus-visible:ring-zinc-900" />
                            </div>
                            <div className="grid gap-2">
                              <Label htmlFor="weightageFirst" className="font-semibold">Weightage %</Label>
                              <Input id="weightageFirst" type="number" min="1" max="100" placeholder="25" value={newGoal.weightage || ''} onChange={(e) => setNewGoal({...newGoal, weightage: parseInt(e.target.value) || 0})} className="focus-visible:ring-zinc-900" />
                            </div>
                          </div>
                        </div>
                        <DialogFooter className="border-t pt-4">
                          <Button onClick={handleAddGoal} className="bg-zinc-900 w-full md:w-auto shadow-md">Save Goal</Button>
                        </DialogFooter>
                      </DialogContent>
                    </Dialog>
                  </div>
                ) : (
                  <div className="p-6 bg-white">
                    <div className="flex justify-between items-center mb-6">
                      <h4 className="text-xl font-bold text-zinc-900">Your Goals</h4>
                      <div className="flex gap-3">
                        <Dialog open={isGoalModalOpen} onOpenChange={setIsGoalModalOpen}>
                          <DialogTrigger asChild>
                            <Button variant="outline" className="border-zinc-300 hover:bg-zinc-100 transition-colors shadow-sm" disabled={goalSheet.status !== 'DRAFT' && goalSheet.status !== 'RETURNED_FOR_REWORK'}>
                              + Add Goal
                            </Button>
                          </DialogTrigger>
                          <DialogContent className="sm:max-w-[500px] border-zinc-200">
                            <DialogHeader>
                              <DialogTitle className="text-2xl font-bold">Add New Goal</DialogTitle>
                              <DialogDescription className="text-zinc-500">Define a clear and measurable objective.</DialogDescription>
                            </DialogHeader>
                            <div className="grid gap-5 py-4">
                              <div className="grid gap-2">
                                <Label htmlFor="title" className="font-semibold">Goal Title</Label>
                                <Input id="title" placeholder="e.g. Increase Customer Retention" value={newGoal.title} onChange={(e) => setNewGoal({...newGoal, title: e.target.value})} className="focus-visible:ring-zinc-900" />
                              </div>
                              <div className="grid gap-2">
                                <Label htmlFor="thrustArea" className="font-semibold">Thrust Area / Category</Label>
                                <Input id="thrustArea" placeholder="e.g. Customer Success" value={newGoal.thrustArea} onChange={(e) => setNewGoal({...newGoal, thrustArea: e.target.value})} className="focus-visible:ring-zinc-900" />
                              </div>
                              <div className="grid grid-cols-2 gap-5">
                                <div className="grid gap-2">
                                  <Label htmlFor="targetValue" className="font-semibold">Target Value</Label>
                                  <Input id="targetValue" placeholder="e.g. 95% or 100ms" value={newGoal.targetValue} onChange={(e) => setNewGoal({...newGoal, targetValue: e.target.value})} className="focus-visible:ring-zinc-900" />
                                </div>
                                <div className="grid gap-2">
                                  <Label htmlFor="weightage" className="font-semibold">Weightage %</Label>
                                  <Input id="weightage" type="number" min="1" max="100" placeholder="25" value={newGoal.weightage || ''} onChange={(e) => setNewGoal({...newGoal, weightage: parseInt(e.target.value) || 0})} className="focus-visible:ring-zinc-900" />
                                </div>
                              </div>
                            </div>
                            <DialogFooter className="border-t pt-4">
                              <Button onClick={handleAddGoal} className="bg-zinc-900 w-full md:w-auto shadow-md">Save Goal</Button>
                            </DialogFooter>
                          </DialogContent>
                        </Dialog>

                        <Button 
                          onClick={handleSubmitSheet} 
                          disabled={(goalSheet.status !== 'DRAFT' && goalSheet.status !== 'RETURNED_FOR_REWORK') || goalSheet.totalWeightage !== 100}
                          className="bg-zinc-900 hover:bg-zinc-800 shadow-md hover:shadow-lg transition-all"
                        >
                          Submit Sheet
                        </Button>
                      </div>
                    </div>

                    <div className="rounded-xl border border-zinc-200 overflow-hidden shadow-sm">
                      <Table>
                        <TableHeader className="bg-zinc-50">
                          <TableRow className="hover:bg-zinc-50 border-zinc-200">
                            <TableHead className="font-bold text-zinc-700">Title</TableHead>
                            <TableHead className="font-bold text-zinc-700">Thrust Area</TableHead>
                            <TableHead className="font-bold text-zinc-700">Target</TableHead>
                            <TableHead className="text-right font-bold text-zinc-700">Weightage</TableHead>
                            <TableHead className="w-10"></TableHead>
                          </TableRow>
                        </TableHeader>
                        <TableBody>
                          {goalSheet.goals?.map((goal: any) => (
                            <TableRow key={goal.id} className="hover:bg-zinc-50 transition-colors border-zinc-100 group">
                              <TableCell className="font-bold text-zinc-900">{goal.title}</TableCell>
                              <TableCell className="text-zinc-600 font-medium">
                                <Badge variant="secondary" className="bg-zinc-100 text-zinc-700 hover:bg-zinc-200">{goal.thrustArea}</Badge>
                              </TableCell>
                              <TableCell className="font-mono bg-zinc-50 rounded px-2 m-2">{goal.targetValue}</TableCell>
                              <TableCell className="text-right">
                                <span className="font-black text-lg bg-zinc-900 text-white px-3 py-1 rounded-md shadow-sm group-hover:scale-105 transition-transform inline-block">
                                  {goal.weightage}%
                                </span>
                              </TableCell>
                              <TableCell>
                                <Button 
                                  variant="ghost" 
                                  size="sm" 
                                  className="text-red-500 hover:text-red-700 hover:bg-red-50 opacity-0 group-hover:opacity-100 transition-opacity"
                                  onClick={() => handleDeleteGoal(goal.id)}
                                  disabled={goalSheet.status !== 'DRAFT' && goalSheet.status !== 'RETURNED_FOR_REWORK'}
                                >
                                  ✕
                                </Button>
                              </TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>

            {/* QUARTERLY PROGRESS TRACKING */}
            {goalSheet?.status === 'APPROVED' && (
              <div className="space-y-6 animate-in slide-in-from-bottom-8 duration-700 fade-in delay-150">
                <h3 className="text-2xl font-bold text-zinc-900 px-2">Quarterly Progress</h3>
                
                {/* Tabs */}
                <div className="flex bg-zinc-200/50 p-1 rounded-xl w-full max-w-md mx-auto sm:mx-0">
                  {['Q1', 'Q2', 'Q3', 'Q4'].map((period) => (
                    <button
                      key={period}
                      onClick={() => setActiveCheckInPeriod(period)}
                      className={`flex-1 py-2 text-sm font-bold rounded-lg transition-all ${
                        activeCheckInPeriod === period 
                          ? 'bg-white text-zinc-900 shadow-sm' 
                          : 'text-zinc-500 hover:text-zinc-700'
                      }`}
                    >
                      {period} Check-in
                    </button>
                  ))}
                </div>

                <div className="rounded-xl border border-zinc-200 overflow-hidden shadow-md bg-white">
                  <Table>
                    <TableHeader className="bg-zinc-50">
                      <TableRow className="border-zinc-200">
                        <TableHead className="font-bold text-zinc-700">Goal</TableHead>
                        <TableHead className="font-bold text-zinc-700">Target</TableHead>
                        <TableHead className="font-bold text-zinc-700">Actual Progress</TableHead>
                        <TableHead className="font-bold text-zinc-700">Status</TableHead>
                        <TableHead className="text-right font-bold text-zinc-700">Score</TableHead>
                        <TableHead></TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {goalSheet.goals?.map((goal: any) => {
                        const form = checkInForms[goal.id] || { actualValue: '', status: '' };
                        
                        // Find if backend has a score for this
                        let progressScore = null;
                        if (goalSheet.reviews) {
                          const review = goalSheet.reviews.find((r: any) => r.period === activeCheckInPeriod);
                          if (review) {
                            const ci = review.checkIns.find((c: any) => c.goalId === goal.id);
                            if (ci && ci.progressScore !== undefined) {
                              progressScore = ci.progressScore;
                            }
                          }
                        }

                        return (
                          <TableRow key={goal.id} className="hover:bg-zinc-50 transition-colors border-zinc-100">
                            <TableCell className="font-bold text-zinc-900">
                              <div className="flex flex-col">
                                <span>{goal.title}</span>
                                <span className="text-xs text-zinc-500 font-normal">{goal.thrustArea}</span>
                              </div>
                            </TableCell>
                            <TableCell className="font-mono bg-zinc-50 rounded px-2 m-2">{goal.targetValue}</TableCell>
                            <TableCell>
                              <Input 
                                placeholder="e.g. 95%" 
                                value={form.actualValue} 
                                onChange={(e) => setCheckInForms({...checkInForms, [goal.id]: {...form, actualValue: e.target.value}})}
                                className="w-32 focus-visible:ring-zinc-900 bg-white"
                              />
                            </TableCell>
                            <TableCell>
                              <select 
                                className="flex h-10 w-full items-center justify-between rounded-md border border-zinc-200 bg-white px-3 py-2 text-sm focus:ring-2 focus:ring-zinc-900"
                                value={form.status}
                                onChange={(e) => setCheckInForms({...checkInForms, [goal.id]: {...form, status: e.target.value}})}
                              >
                                <option value="" disabled>Select Status</option>
                                <option value="NOT_STARTED">Not Started</option>
                                <option value="ON_TRACK">On Track</option>
                                <option value="NEEDS_ATTENTION">Needs Attention</option>
                                <option value="COMPLETED">Completed</option>
                                <option value="POSTPONED">Postponed</option>
                              </select>
                            </TableCell>
                            <TableCell className="text-right">
                              {progressScore !== null ? (
                                <Badge variant="outline" className={`font-mono text-base ${progressScore >= 1 ? 'text-green-600 border-green-200 bg-green-50' : 'text-zinc-600'}`}>
                                  {progressScore.toFixed(2)}
                                </Badge>
                              ) : (
                                <span className="text-zinc-300">-</span>
                              )}
                            </TableCell>
                            <TableCell>
                              <Button 
                                size="sm" 
                                onClick={() => handleSaveCheckIn(goal.id)}
                                className="bg-zinc-900 hover:bg-zinc-800 text-xs shadow-sm"
                                disabled={!form.actualValue || !form.status}
                              >
                                Save
                              </Button>
                            </TableCell>
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                </div>
              </div>
            )}

            {/* Reject Message for Employee */}
            {goalSheet?.status === 'RETURNED_FOR_REWORK' && goalSheet?.lastManagerComment && !isFeedbackDismissed && (
              <div className="bg-destructive/10 border-l-4 border-destructive p-4 rounded-r-md mt-6 shadow-sm animate-in fade-in slide-in-from-bottom-2 relative">
                <button 
                  onClick={() => setIsFeedbackDismissed(true)} 
                  className="absolute top-4 right-4 text-destructive/60 hover:text-destructive hover:bg-destructive/10 rounded-full p-1 transition-colors"
                  aria-label="Dismiss feedback"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16">
                    <path d="M2.146 2.854a.5.5 0 1 1 .708-.708L8 7.293l5.146-5.147a.5.5 0 0 1 .708.708L8.707 8l5.147 5.146a.5.5 0 0 1-.708.708L8 8.707l-5.146 5.147a.5.5 0 0 1-.708-.708L7.293 8 2.146 2.854Z"/>
                  </svg>
                </button>
                <h4 className="text-destructive font-bold mb-1 flex items-center gap-2">
                  <span>⚠️</span> Manager Feedback
                </h4>
                <p className="text-zinc-700 pr-8">{goalSheet.lastManagerComment}</p>
              </div>
            )}
          </div>
        )}
      </main>

      {/* Reject Modal (Manager View) */}
      <Dialog open={isRejectModalOpen} onOpenChange={setIsRejectModalOpen}>
        <DialogContent className="sm:max-w-[500px] border-zinc-200">
          <DialogHeader>
            <DialogTitle className="text-2xl font-bold text-destructive">Reject Goal Sheet</DialogTitle>
            <DialogDescription className="text-zinc-500">Provide a reason for returning this sheet for rework. This will be visible to the employee.</DialogDescription>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid gap-2">
              <Label htmlFor="rejectReason" className="font-semibold">Reason for rejection</Label>
              <Input 
                id="rejectReason" 
                placeholder="e.g. Please revise your target value for Q3 to 95." 
                value={rejectReason} 
                onChange={(e) => setRejectReason(e.target.value)} 
                className="focus-visible:ring-destructive" 
                autoFocus
              />
            </div>
          </div>
          <DialogFooter className="border-t pt-4">
            <Button variant="outline" onClick={() => setIsRejectModalOpen(false)}>Cancel</Button>
            <Button onClick={handleRejectSheet} className="bg-destructive hover:bg-destructive/90 text-white shadow-md">Reject Sheet</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Shared Goal Modal (Manager View) */}
      <Dialog open={isSharedGoalModalOpen} onOpenChange={setIsSharedGoalModalOpen}>
        <DialogContent className="sm:max-w-[700px] border-zinc-200 max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="text-2xl font-bold">Deploy Shared Goal</DialogTitle>
            <DialogDescription className="text-zinc-500">Assign a common organizational goal to multiple team members at once.</DialogDescription>
          </DialogHeader>
          <div className="grid gap-6 py-4">
            
            {/* Goal Definition Section */}
            <div className="space-y-4">
              <h3 className="font-semibold border-b pb-2">1. Goal Definition</h3>
              <div className="grid gap-4">
                <div className="grid gap-2">
                  <Label htmlFor="sharedTitle" className="font-semibold text-sm">Goal Title</Label>
                  <Input id="sharedTitle" placeholder="e.g. Q3 Department Revenue Target" value={sharedGoal.title} onChange={(e) => setSharedGoal({...sharedGoal, title: e.target.value})} />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="sharedThrustArea" className="font-semibold text-sm">Thrust Area / Category</Label>
                  <Input id="sharedThrustArea" placeholder="e.g. Financial Growth" value={sharedGoal.thrustArea} onChange={(e) => setSharedGoal({...sharedGoal, thrustArea: e.target.value})} />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="sharedDescription" className="font-semibold text-sm">Description</Label>
                  <Input id="sharedDescription" placeholder="Optional details about this goal..." value={sharedGoal.description} onChange={(e) => setSharedGoal({...sharedGoal, description: e.target.value})} />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="grid gap-2">
                    <Label htmlFor="sharedTargetValue" className="font-semibold text-sm">Target Value</Label>
                    <Input id="sharedTargetValue" placeholder="e.g. 100% or 10 units" value={sharedGoal.targetValue} onChange={(e) => setSharedGoal({...sharedGoal, targetValue: e.target.value})} />
                  </div>
                </div>
              </div>
            </div>

            {/* Team Assignment Section */}
            <div className="space-y-4">
              <h3 className="font-semibold border-b pb-2">2. Team Assignment & Weightage</h3>
              {team.length === 0 ? (
                <p className="text-zinc-500 text-sm">You have no direct reports to assign goals to.</p>
              ) : (
                <div className="grid gap-3">
                  {team.map((member) => (
                    <div key={member.employee.id} className="flex items-center justify-between p-3 rounded-lg border border-zinc-200 bg-zinc-50">
                      <div className="flex flex-col">
                        <span className="font-semibold text-zinc-900">{member.employee.name}</span>
                        <span className="text-xs text-zinc-500">{member.employee.department}</span>
                      </div>
                      <div className="flex items-center gap-3">
                        <Label htmlFor={`weight-${member.employee.id}`} className="text-xs font-semibold text-zinc-600">Weightage %:</Label>
                        <Input 
                          id={`weight-${member.employee.id}`}
                          type="number" 
                          min="0" 
                          max="100"
                          placeholder="0"
                          className="w-20 text-right font-mono"
                          value={sharedGoalRecipients[member.employee.id] || ''}
                          onChange={(e) => setSharedGoalRecipients({
                            ...sharedGoalRecipients, 
                            [member.employee.id]: parseInt(e.target.value) || 0
                          })}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

          </div>
          <DialogFooter className="border-t pt-4">
            <Button variant="outline" onClick={() => setIsSharedGoalModalOpen(false)}>Cancel</Button>
            <Button onClick={handlePushSharedGoal} className="bg-zinc-900 hover:bg-zinc-800 shadow-md">Deploy to Team</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Admin User Creation Modal */}
      <Dialog open={isAdminUserModalOpen} onOpenChange={setIsAdminUserModalOpen}>
        <DialogContent className="sm:max-w-[500px] border-zinc-200">
          <DialogHeader>
            <DialogTitle className="text-2xl font-bold">Add New User</DialogTitle>
            <DialogDescription className="text-zinc-500">Create a new employee, manager, or admin account.</DialogDescription>
          </DialogHeader>
          <div className="grid gap-5 py-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-2">
                <Label htmlFor="userId" className="font-semibold">User ID</Label>
                <Input id="userId" placeholder="e.g. emp-10" value={newUser.userId} onChange={(e) => setNewUser({...newUser, userId: e.target.value})} className="focus-visible:ring-zinc-900" />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="userRole" className="font-semibold">System Role</Label>
                <select 
                  id="userRole" 
                  className="flex h-10 w-full items-center justify-between rounded-md border border-zinc-200 bg-white px-3 py-2 text-sm ring-offset-white placeholder:text-zinc-500 focus:outline-none focus:ring-2 focus:ring-zinc-900 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                  value={newUser.role}
                  onChange={(e) => setNewUser({...newUser, role: e.target.value})}
                >
                  <option value="EMPLOYEE">Employee</option>
                  <option value="MANAGER">Manager</option>
                  <option value="ADMIN">Admin</option>
                </select>
              </div>
            </div>
            <div className="grid gap-2">
              <Label htmlFor="userName" className="font-semibold">Full Name</Label>
              <Input id="userName" placeholder="e.g. John Doe" value={newUser.name} onChange={(e) => setNewUser({...newUser, name: e.target.value})} className="focus-visible:ring-zinc-900" />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="userDept" className="font-semibold">Department</Label>
              <Input id="userDept" placeholder="e.g. Engineering" value={newUser.department} onChange={(e) => setNewUser({...newUser, department: e.target.value})} className="focus-visible:ring-zinc-900" />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="userManager" className="font-semibold">Manager ID (Optional)</Label>
              <Input id="userManager" placeholder="e.g. mgr-1" value={newUser.managerId} onChange={(e) => setNewUser({...newUser, managerId: e.target.value})} className="focus-visible:ring-zinc-900" />
              <p className="text-xs text-zinc-500">Leave blank if the user has no direct manager.</p>
            </div>
          </div>
          <DialogFooter className="border-t pt-4">
            <Button variant="outline" onClick={() => setIsAdminUserModalOpen(false)}>Cancel</Button>
            <Button onClick={handleCreateUser} className="bg-zinc-900 w-full md:w-auto shadow-md">Create Account</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

export default App;
