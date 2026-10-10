/**
 * AI Interviewer — Client Application Logic
 * Integrates WebRTC media stream, Web Speech API, LiveKit, 15-Minute Global Exam Timer,
 * TensorFlow.js COCO-SSD Camera Object Detection, Fullscreen Lockdown, and Malpractice Proctoring.
 */

const API_BASE = window.location.origin;

// Fixed 15-minute exam duration in seconds (15 * 60 = 900)
const EXAM_DURATION_SECONDS = 15 * 60;

// Application State
const state = {
    sessionId: null,
    candidateId: null,
    candidateName: "Jane Doe",
    currentQuestionNumber: 1,
    totalQuestions: 7,
    currentQuestion: null,

    // Question Turn Timer
    timeRemaining: 120,
    totalDuration: 120,
    timerInterval: null,

    // Global 15-Minute Exam Session Timer
    globalExamDuration: EXAM_DURATION_SECONDS,
    globalExamTimeRemaining: EXAM_DURATION_SECONDS,
    globalExamTimerInterval: null,

    // Media & Speech
    mediaStream: null,
    audioContext: null,
    analyser: null,
    speechRecognition: null,
    isRecognizing: false,
    finalTranscript: "",
    livekitRoom: null,

    // AI Proctoring & Malpractice State
    proctor: {
        active: false,
        warningsCount: 0,
        maxWarnings: 3,
        violations: [],
        cocoModel: null,
        isModelLoading: false,
        detectionInterval: null,
        isDisqualified: false,
        lastViolationTime: 0,
        lastMobileAlertTime: 0,
        toastTimeout: null
    },

    // Supabase Auth State
    supabase: {
        client: null,
        url: "https://eiwqkbnhdzbtqjvlvxji.supabase.co",
        anonKey: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVpd3FrYm5oZHpidHFqdmx2eGppIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTEwOTc1NjcsImV4cCI6MjEwNjY3MzU2N30.4LOyXy-xfBDooJ6-UyRKxBDhWMEmFgLD8kMF66XdOEw",
        isConfigured: true,
        user: null,
        session: null
    },
    authRole: localStorage.getItem('user_role') || 'STUDENT',

    // Admin Scheduling & Candidate Assignment State
    admin: {
        schedules: [],
        selectedInterviewId: null
    },
    candidateSchedule: {
        assignedInterviews: [],
        activeAssignment: null,
        sessionToken: sessionStorage.getItem('active_session_token') || null
    }
};

// DOM Elements
const elements = {
    stepLogin: document.getElementById('step-login'),
    stepSetup: document.getElementById('step-setup'),
    stepInterview: document.getElementById('step-interview'),
    stepScorecard: document.getElementById('step-scorecard'),
    stepMalpractice: document.getElementById('step-malpractice'),

    // Dedicated Login Page Elements
    tabLoginStudent: document.getElementById('tab-login-student'),
    tabLoginAdmin: document.getElementById('tab-login-admin'),
    formLoginPageStudent: document.getElementById('form-login-page-student'),
    formLoginPageAdmin: document.getElementById('form-login-page-admin'),
    loginStudentId: document.getElementById('login-student-id'),
    loginStudentPwd: document.getElementById('login-student-pwd'),
    loginAdminId: document.getElementById('login-admin-id'),
    loginAdminPwd: document.getElementById('login-admin-pwd'),
    btnChipDemoStudent: document.getElementById('btn-chip-demo-student'),
    btnChipDemoAdmin: document.getElementById('btn-chip-demo-admin'),
    btnSubmitPageStudent: document.getElementById('btn-submit-page-student'),
    btnSubmitPageAdmin: document.getElementById('btn-submit-page-admin'),
    loginPageFeedback: document.getElementById('login-page-feedback'),
    btnPageForgotPw: document.getElementById('btn-page-forgot-pw'),

    candidateForm: document.getElementById('candidate-form'),
    candidateNameInput: document.getElementById('candidate-name'),
    resumeTextInput: document.getElementById('resume-text'),
    btnLoadSample: document.getElementById('btn-load-sample'),
    
    // Video & Media
    localVideo: document.getElementById('local-video'),
    proctorCanvas: document.getElementById('proctor-canvas'),
    proctorCamTag: document.getElementById('proctor-cam-tag'),
    proctorCamStatusText: document.getElementById('proctor-cam-status-text'),
    userAvatarInitial: document.getElementById('user-avatar-initial'),
    videoCandidateName: document.getElementById('video-candidate-name'),
    waveformCanvas: document.getElementById('waveform-canvas'),
    livekitRoomTag: document.getElementById('livekit-room-tag'),
    livekitStatusPill: document.getElementById('livekit-status-pill'),
    
    // Header Proctor HUD
    proctorStatusHud: document.getElementById('proctor-status-hud'),
    proctorWarningsBadge: document.getElementById('proctor-warnings-badge'),

    // Question HUD & Timers
    hudQNumber: document.getElementById('hud-q-number'),
    hudQCategory: document.getElementById('hud-q-category'),
    hudQTopic: document.getElementById('hud-q-topic'),
    hudQText: document.getElementById('hud-q-text'),
    btnReplayAudio: document.getElementById('btn-replay-audio'),
    liveTranscript: document.getElementById('live-transcript'),
    sessionGlobalTimer: document.getElementById('session-global-timer'),
    globalTimerVal: document.getElementById('global-timer-val'),
    timerWidget: document.getElementById('timer-widget'),
    timerRingProgress: document.getElementById('timer-ring-progress'),
    hudTimerText: document.getElementById('hud-timer-text'),
    btnSubmitTurn: document.getElementById('btn-submit-turn'),
    btnSubmitLabel: document.getElementById('btn-submit-label'),
    progressBar: document.getElementById('interview-progress-bar'),
    
    // Upload Dropzone
    uploadDropzone: document.getElementById('upload-dropzone'),
    resumeFileInput: document.getElementById('resume-file-input'),
    dropzonePrompt: document.getElementById('dropzone-prompt'),
    dropzoneLoading: document.getElementById('dropzone-loading'),
    skillsPreviewGroup: document.getElementById('skills-preview-group'),
    parsedSkillsChips: document.getElementById('parsed-skills-chips'),

    // Scorecard
    reportCandidateName: document.getElementById('report-candidate-name'),
    reportVerdictBadge: document.getElementById('report-verdict-badge'),
    reportVerdictText: document.getElementById('report-verdict-text'),
    scoreOverall: document.getElementById('score-overall'),
    scoreTech: document.getElementById('score-tech'),
    scoreComm: document.getElementById('score-comm'),
    barOverall: document.getElementById('bar-overall'),
    barTech: document.getElementById('bar-tech'),
    barComm: document.getElementById('bar-comm'),
    reportDetailedFeedback: document.getElementById('report-detailed-feedback'),
    reportTurnsList: document.getElementById('report-turns-list'),
    btnRestart: document.getElementById('btn-restart'),
    btnPrintReport: document.getElementById('btn-print-report'),

    // Malpractice & Proctor Overlays
    proctorAlertToast: document.getElementById('proctor-alert-toast'),
    proctorToastTitle: document.getElementById('proctor-toast-title'),
    proctorToastMessage: document.getElementById('proctor-toast-message'),
    proctorToastCount: document.getElementById('proctor-toast-count'),
    fullscreenLockoutOverlay: document.getElementById('fullscreen-lockout-overlay'),
    btnResumeFullscreen: document.getElementById('btn-resume-fullscreen'),
    malpracticeCandidateTitle: document.getElementById('malpractice-candidate-title'),
    malpracticeReasonText: document.getElementById('malpractice-reason-text'),
    malpracticeTotalWarnings: document.getElementById('malpractice-total-warnings'),
    malpracticeTimestamp: document.getElementById('malpractice-timestamp'),
    malpracticeAuditTbody: document.getElementById('malpractice-audit-tbody'),
    btnRestartMalpractice: document.getElementById('btn-restart-malpractice'),

    // Supabase Header & Status Elements
    btnSupabaseConfig: document.getElementById('btn-supabase-config'),
    sbStatusDot: document.getElementById('sb-status-dot'),
    sbStatusText: document.getElementById('sb-status-text'),
    userRoleBadge: document.getElementById('user-role-badge'),
    userRoleIcon: document.getElementById('user-role-icon'),
    userRoleText: document.getElementById('user-role-text'),
    adminWelcomeBanner: document.getElementById('admin-welcome-banner'),
    btnBannerOpenAdmin: document.getElementById('btn-banner-open-admin'),
    authUnauthenticated: document.getElementById('auth-unauthenticated'),
    btnOpenAuthModal: document.getElementById('btn-open-auth-modal'),
    authAuthenticated: document.getElementById('auth-authenticated'),
    userAvatarPill: document.getElementById('user-avatar-pill'),
    userDisplayName: document.getElementById('user-display-name'),
    userDisplayEmail: document.getElementById('user-display-email'),
    btnSignout: document.getElementById('btn-signout'),

    // Supabase Auth Modal Elements
    modalSupabaseAuth: document.getElementById('modal-supabase-auth'),
    btnCloseAuthModal: document.getElementById('btn-close-auth-modal'),
    authMissingKeyBanner: document.getElementById('auth-missing-key-banner'),
    btnGoToConfig: document.getElementById('btn-go-to-config'),
    btnRoleStudent: document.getElementById('btn-role-student'),
    btnRoleAdmin: document.getElementById('btn-role-admin'),
    labelSigninEmail: document.getElementById('label-signin-email'),
    labelSigninPassword: document.getElementById('label-signin-password'),
    btnSubmitSigninText: document.getElementById('btn-submit-signin-text'),
    demoCredentialsHelper: document.getElementById('demo-credentials-helper'),
    demoCredentialsText: document.getElementById('demo-credentials-text'),
    btnAutofillDemo: document.getElementById('btn-autofill-demo'),
    tabBtnSignin: document.getElementById('tab-btn-signin'),
    tabBtnSignup: document.getElementById('tab-btn-signup'),
    tabBtnMagic: document.getElementById('tab-btn-magic'),
    formAuthSignin: document.getElementById('form-auth-signin'),
    authSigninEmail: document.getElementById('auth-signin-email'),
    authSigninPassword: document.getElementById('auth-signin-password'),
    btnSubmitSignin: document.getElementById('btn-submit-signin'),
    btnShowForgotPw: document.getElementById('btn-show-forgot-pw'),
    formAuthReset: document.getElementById('form-auth-reset'),
    authResetEmail: document.getElementById('auth-reset-email'),
    authResetPassword: document.getElementById('auth-reset-password'),
    authResetConfirm: document.getElementById('auth-reset-confirm'),
    btnSubmitResetPw: document.getElementById('btn-submit-reset-pw'),
    btnBackToSignin: document.getElementById('btn-back-to-signin'),
    authResetFeedback: document.getElementById('auth-reset-feedback'),
    formAuthSignup: document.getElementById('form-auth-signup'),
    authSignupName: document.getElementById('auth-signup-name'),
    authSignupEmail: document.getElementById('auth-signup-email'),
    authSignupPassword: document.getElementById('auth-signup-password'),
    btnSubmitSignup: document.getElementById('btn-submit-signup'),
    formAuthMagic: document.getElementById('form-auth-magic'),
    authMagicEmail: document.getElementById('auth-magic-email'),
    btnSubmitMagic: document.getElementById('btn-submit-magic'),
    btnOauthGithub: document.getElementById('btn-oauth-github'),
    btnOauthGoogle: document.getElementById('btn-oauth-google'),
    authFeedbackBox: document.getElementById('auth-feedback-box'),

    // Supabase Config Modal Elements
    modalSupabaseConfig: document.getElementById('modal-supabase-config'),
    btnCloseConfigModal: document.getElementById('btn-close-config-modal'),
    formSupabaseConfig: document.getElementById('form-supabase-config'),
    sbInputUrl: document.getElementById('sb-input-url'),
    sbInputAnonKey: document.getElementById('sb-input-anon-key'),
    btnSaveSbConfig: document.getElementById('btn-save-sb-config'),
    btnClearSbConfig: document.getElementById('btn-clear-sb-config'),
    sbConfigFeedback: document.getElementById('sb-config-feedback'),

    // Admin Portal Elements
    modalAdminPortal: document.getElementById('modal-admin-portal'),
    btnCloseAdminPortal: document.getElementById('btn-close-admin-portal'),
    tabBtnSchedule: document.getElementById('tab-btn-schedule'),
    tabBtnAssign: document.getElementById('tab-btn-assign'),
    tabBtnMarks: document.getElementById('tab-btn-marks'),
    panelAdminSchedule: document.getElementById('panel-admin-schedule'),
    panelAdminAssign: document.getElementById('panel-admin-assign'),
    panelAdminMarks: document.getElementById('panel-admin-marks'),
    
    // Schedule Form
    formCreateSchedule: document.getElementById('form-create-schedule'),
    schedTitle: document.getElementById('sched-title'),
    schedRole: document.getElementById('sched-role'),
    schedDuration: document.getElementById('sched-duration'),
    schedQuestions: document.getElementById('sched-questions'),
    schedTargetYear: document.getElementById('sched-target-year'),
    schedDescription: document.getElementById('sched-description'),
    scheduleFeedback: document.getElementById('schedule-feedback'),
    activeSchedulesList: document.getElementById('active-schedules-list'),

    // Permanent Student Credentials Elements
    formCreateStudent: document.getElementById('form-create-student'),
    studentName: document.getElementById('student-name'),
    studentRollNo: document.getElementById('student-roll-no'),
    studentEmail: document.getElementById('student-email'),
    studentYear: document.getElementById('student-year'),
    studentDept: document.getElementById('student-dept'),
    studentPassword: document.getElementById('student-password'),
    btnGenerateStudentPw: document.getElementById('btn-generate-student-pw'),
    btnSubmitCreateStudent: document.getElementById('btn-submit-create-student'),
    createStudentFeedback: document.getElementById('create-student-feedback'),
    credYearDept: document.getElementById('cred-year-dept'),

    // Year Assignment & Single Candidate Elements
    subtabAssignYear: document.getElementById('subtab-assign-year'),
    subtabAssignSingle: document.getElementById('subtab-assign-single'),
    formAssignYear: document.getElementById('form-assign-year'),
    assignYearInterviewSelect: document.getElementById('assign-year-interview-select'),
    assignTargetYearSelect: document.getElementById('assign-target-year-select'),
    btnSubmitAssignYear: document.getElementById('btn-submit-assign-year'),
    assignYearFeedback: document.getElementById('assign-year-feedback'),

    formAssignCandidate: document.getElementById('form-assign-candidate'),
    assignInterviewSelect: document.getElementById('assign-interview-select'),
    assignCandidateName: document.getElementById('assign-candidate-name'),
    assignCandidateRollNo: document.getElementById('assign-candidate-roll-no'),
    assignCandidateEmail: document.getElementById('assign-candidate-email'),
    assignCandidatePassword: document.getElementById('assign-candidate-password'),
    btnGeneratePassword: document.getElementById('btn-generate-password'),
    assignFeedback: document.getElementById('assign-feedback'),
    credentialsCard: document.getElementById('credentials-card'),
    credDriveTitle: document.getElementById('cred-drive-title'),
    credName: document.getElementById('cred-name'),
    credRollNo: document.getElementById('cred-roll-no'),
    credEmail: document.getElementById('cred-email'),
    credPassword: document.getElementById('cred-password'),
    btnCopyCredentials: document.getElementById('btn-copy-credentials'),

    // Attendance & Marks Review Elements
    marksInterviewSelect: document.getElementById('marks-interview-select'),
    btnRefreshMarks: document.getElementById('btn-refresh-marks'),
    btnExportExcel: document.getElementById('btn-export-excel'),
    statTotalAssigned: document.getElementById('stat-total-assigned'),
    statTotalCompleted: document.getElementById('stat-total-completed'),
    statTotalMalpractice: document.getElementById('stat-total-malpractice'),
    statAvgScore: document.getElementById('stat-avg-score'),
    attendanceMarksTable: document.getElementById('attendance-marks-table'),
    attendanceMarksTbody: document.getElementById('attendance-marks-tbody'),

    // Scorecard Modal
    modalAdminScorecard: document.getElementById('modal-admin-scorecard'),
    btnCloseAdminScorecard: document.getElementById('btn-close-admin-scorecard'),
    adminScorecardCandidate: document.getElementById('admin-scorecard-candidate'),
    adminScorecardVerdict: document.getElementById('admin-scorecard-verdict'),
    adminScorecardContent: document.getElementById('admin-scorecard-content'),

    // Candidate Assigned Interviews
    candidateAssignedSection: document.getElementById('candidate-assigned-interviews-section'),
    assignedInterviewsCount: document.getElementById('assigned-interviews-count'),
    pastInterviewsGrid: document.getElementById('past-interviews-grid'),
    pastSectionHeader: document.getElementById('past-section-header'),
    upcomingLabel: document.getElementById('upcoming-label'),
    practiceInterviewCard: document.getElementById('practice-interview-card'),
    btnStartPractice: document.getElementById('btn-start-practice'),
    resumeGateCard: document.getElementById('resume-gate-card'),
    resumeGateTitle: document.getElementById('resume-gate-title'),
    resumeGateDesc: document.getElementById('resume-gate-desc'),
    assignedInterviewsGrid: document.getElementById('assigned-interviews-grid'),
    activeDrivePillBox: document.getElementById('active-drive-pill-box'),
    activeDriveTitle: document.getElementById('active-drive-title'),
    activeDriveLimits: document.getElementById('active-drive-limits'),
    btnClearActiveDrive: document.getElementById('btn-clear-active-drive'),

    // Change Password Modal
    btnOpenChangePw: document.getElementById('btn-open-change-pw'),
    modalChangePassword: document.getElementById('modal-change-password'),
    btnCloseChangePw: document.getElementById('btn-close-change-pw'),
    formChangePassword: document.getElementById('form-change-password'),
    inputNewPassword: document.getElementById('input-new-password'),
    inputConfirmPassword: document.getElementById('input-confirm-password'),
    changePwFeedback: document.getElementById('change-pw-feedback')
};

// ----------------------------------------------------
// INITIALIZATION & EVENT LISTENERS
// ----------------------------------------------------
document.addEventListener('DOMContentLoaded', () => {
    initSampleResume();
    setupEventListeners();
    setupDropzoneListeners();
    setupSpeechRecognition();
    setupProctorEventListeners();
    preloadCocoSsdModel();
    initSupabaseAuth();
    initAdminAndScheduling();
    initPortalNavigation();
});

function setupEventListeners() {
    elements.candidateForm.addEventListener('submit', handleStartInterview);
    elements.btnLoadSample.addEventListener('click', loadSampleResume);
    elements.btnSubmitTurn.addEventListener('click', () => submitCurrentTurn(false));
    elements.btnReplayAudio.addEventListener('click', speakCurrentQuestion);
    elements.btnRestart.addEventListener('click', resetToSetup);
    elements.btnPrintReport.addEventListener('click', () => window.print());
    
    if (elements.btnResumeFullscreen) {
        elements.btnResumeFullscreen.addEventListener('click', resumeFullscreenMode);
    }
    if (elements.btnRestartMalpractice) {
        elements.btnRestartMalpractice.addEventListener('click', resetToSetup);
    }

    setupSupabaseEventListeners();
    setupAdminEventListeners();
    setupLoginPageListeners();

    const brand = document.querySelector('.brand');
    if (brand) {
        brand.style.cursor = 'pointer';
        brand.addEventListener('click', () => {
            if (state.supabase.user) {
                switchStep('step-setup');
            } else {
                switchStep('step-login');
            }
        });
    }
}

function setupDropzoneListeners() {
    const dropzone = elements.uploadDropzone;
    const fileInput = elements.resumeFileInput;

    dropzone.addEventListener('click', () => fileInput.click());

    fileInput.addEventListener('change', (e) => {
        if (e.target.files && e.target.files[0]) {
            handleFileUpload(e.target.files[0]);
        }
    });

    ['dragenter', 'dragover'].forEach(eventName => {
        dropzone.addEventListener(eventName, (e) => {
            e.preventDefault();
            dropzone.classList.add('dragover');
        });
    });

    ['dragleave', 'drop'].forEach(eventName => {
        dropzone.addEventListener(eventName, (e) => {
            e.preventDefault();
            dropzone.classList.remove('dragover');
        });
    });

    dropzone.addEventListener('drop', (e) => {
        if (e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files[0]) {
            handleFileUpload(e.dataTransfer.files[0]);
        }
    });
}

async function handleFileUpload(file) {
    console.log("Uploading and parsing resume file:", file.name);
    elements.dropzonePrompt.style.display = 'none';
    elements.dropzoneLoading.style.display = 'flex';

    try {
        const formData = new FormData();
        formData.append('file', file);

        const response = await fetch(`${API_BASE}/api/resume/upload`, {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            throw new Error(`Upload failed with status ${response.status}`);
        }

        const parsed = await response.json();

        if (parsed.candidateName && parsed.candidateName !== 'Unnamed Candidate' && parsed.candidateName !== 'Standard Candidate') {
            elements.candidateNameInput.value = parsed.candidateName;
        }

        let resumeContent = `${parsed.summary || ''}\n\nTechnical Skills: ${(parsed.technicalSkills || []).join(', ')}\nDomain Areas: ${(parsed.domainAreas || []).join(', ')}`;
        if (parsed.keyProjects && parsed.keyProjects.length > 0) {
            resumeContent += `\nProjects: ${parsed.keyProjects.join('; ')}`;
        }
        elements.resumeTextInput.value = resumeContent.trim();

        renderParsedSkillsChips(parsed.technicalSkills, parsed.domainAreas);

    } catch (err) {
        console.error("Resume file parsing error:", err);
        alert("Could not parse file. Reading as text...");
        const reader = new FileReader();
        reader.onload = (e) => {
            elements.resumeTextInput.value = e.target.result;
        };
        reader.readAsText(file);
    } finally {
        elements.dropzonePrompt.style.display = 'flex';
        elements.dropzoneLoading.style.display = 'none';
    }
}

function renderParsedSkillsChips(skills = [], domains = []) {
    const all = [...new Set([...skills, ...domains])];
    if (all.length === 0) {
        elements.skillsPreviewGroup.style.display = 'none';
        return;
    }

    elements.parsedSkillsChips.innerHTML = '';
    all.forEach(skill => {
        const badge = document.createElement('span');
        badge.className = 'skill-chip-badge';
        badge.textContent = `⚡ ${skill}`;
        elements.parsedSkillsChips.appendChild(badge);
    });

    elements.skillsPreviewGroup.style.display = 'block';
}

function initSampleResume() {
    elements.candidateNameInput.value = "Jane Doe";
}

function loadSampleResume() {
    elements.candidateNameInput.value = "Alex Chen";
    elements.resumeTextInput.value = "2nd-year Computer Science student at University. Skills: Python, Java, React, PostgreSQL, and Git. Coursework: Data Structures & Algorithms, Database Systems. Built a real-time event booking portal with concurrency handling and hash-based search indexing.";
}

// ----------------------------------------------------
// STEP 1: LAUNCH 15-MINUTE PROCTORED INTERVIEW
// ----------------------------------------------------
async function handleStartInterview(e) {
    e.preventDefault();
    const name = elements.candidateNameInput.value.trim();
    const resumeText = elements.resumeTextInput.value.trim();
    const fixedTotalQuestions = 7; // 15-minute comprehensive technical roadmap

    if (!name || !resumeText) return;

    state.candidateName = name;
    state.totalQuestions = fixedTotalQuestions;
    elements.videoCandidateName.textContent = name;
    elements.userAvatarInitial.textContent = name.charAt(0).toUpperCase();

    try {
        setButtonLoading(true, 'Starting 15-Min Proctored Exam...');

        // 1. Create candidate (linked to Supabase Auth user if signed in)
        const candPayload = {
            candidateName: name,
            resumeText: resumeText,
            email: (state.supabase && state.supabase.user) ? state.supabase.user.email : null,
            authUserId: (state.supabase && state.supabase.user) ? state.supabase.user.id : null
        };
        const candRes = await fetch(`${API_BASE}/api/resume/candidates`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(candPayload)
        });
        const candidate = await candRes.json();
        state.candidateId = candidate.id;

        // 2. Start Interview Session (with assignment & network resilience token)
        const assignment = state.candidateSchedule.activeAssignment;
        const totalQ = assignment && assignment.totalQuestions ? assignment.totalQuestions : fixedTotalQuestions;
        const assignmentId = assignment ? assignment.assignmentId : null;
        let sessionToken = state.candidateSchedule.sessionToken;
        if (!sessionToken && assignment) {
            sessionToken = 'sess_' + Math.random().toString(36).substring(2) + Date.now();
            state.candidateSchedule.sessionToken = sessionToken;
        }

        const startRes = await fetch(`${API_BASE}/api/interview/start`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                candidateId: candidate.id,
                totalQuestions: totalQ,
                assignmentId: assignmentId,
                sessionToken: sessionToken
            })
        });

        if (!startRes.ok) {
            const errData = await startRes.json().catch(() => ({}));
            throw new Error(errData.message || `Failed to start session (Status ${startRes.status})`);
        }

        const session = await startRes.json();
        state.sessionId = session.sessionId;

        if (sessionToken) {
            sessionStorage.setItem('active_session_token', sessionToken);
            if (assignmentId) sessionStorage.setItem('active_assignment_id', assignmentId);
        }

        // 3. Initialize Camera & Audio Waveform
        await initLocalMedia();

        // 4. Connect to LiveKit Room if token is present
        if (session.livekit && session.livekit.token) {
            initLiveKit(session.livekit);
        }

        // 5. Enter Fullscreen and Start Proctor Engine
        await requestFullscreenLock();
        startProctorEngine();

        // 6. Start Global 15-Minute Exam Countdown Clock
        startGlobalExamTimer();

        // 7. Render Question #1
        renderQuestion(session.currentQuestion, 1);

        // Transition view
        switchStep('step-interview');
    } catch (err) {
        console.error('Failed to start interview:', err);
        alert('Could not start interview: ' + err.message);
    } finally {
        setButtonLoading(false, 'Launch Voice Interview');
    }
}

// ----------------------------------------------------
// GLOBAL 15-MINUTE INTERVIEW SESSION TIMER
// ----------------------------------------------------
function startGlobalExamTimer() {
    clearInterval(state.globalExamTimerInterval);
    state.globalExamTimeRemaining = state.globalExamDuration;
    updateGlobalTimerDisplay();

    state.globalExamTimerInterval = setInterval(() => {
        state.globalExamTimeRemaining--;
        updateGlobalTimerDisplay();

        if (state.globalExamTimeRemaining <= 0) {
            clearInterval(state.globalExamTimerInterval);
            console.warn("15-Minute interview allocated duration reached! Finalizing assessment...");
            handleExamDurationExpired();
        }
    }, 1000);
}

function updateGlobalTimerDisplay() {
    if (!elements.globalTimerVal) return;
    const mins = Math.floor(Math.max(0, state.globalExamTimeRemaining) / 60);
    const secs = Math.max(0, state.globalExamTimeRemaining) % 60;
    elements.globalTimerVal.textContent = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;

    if (elements.sessionGlobalTimer) {
        if (state.globalExamTimeRemaining <= 120) {
            elements.sessionGlobalTimer.classList.add('urgent');
        } else {
            elements.sessionGlobalTimer.classList.remove('urgent');
        }
    }
}

async function handleExamDurationExpired() {
    stopSpeechSynthesis();
    clearInterval(state.timerInterval);
    clearInterval(state.globalExamTimerInterval);
    stopSpeechRecognition();

    let transcript = elements.liveTranscript.value.trim();
    if (!transcript) {
        transcript = "Allocated 15-minute interview duration expired.";
    }

    try {
        // Submit last turn
        await fetch(`${API_BASE}/api/interview/next`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                sessionId: state.sessionId,
                questionNumber: state.currentQuestionNumber,
                candidateTranscript: transcript,
                isTimeout: true,
                timeTakenSeconds: state.totalDuration - state.timeRemaining
            })
        });
    } catch (e) {
        console.warn("Final turn submission on time expiry:", e);
    }

    // Immediately load final evaluated scorecard
    loadFinalScorecard("Completed: 15-Minute Allocated Duration Reached");
}

// ----------------------------------------------------
// STEP 2: QUESTION HUD, SPEECH, & TURN TIMER LOGIC
// ----------------------------------------------------
function renderQuestion(question, qNum) {
    stopSpeechSynthesis();

    state.currentQuestion = question;
    state.currentQuestionNumber = qNum;

    elements.hudQNumber.textContent = `Question ${qNum} of ${state.totalQuestions}`;
    elements.hudQCategory.textContent = question.category || 'RESUME_SKILL';
    elements.hudQTopic.textContent = question.topic || 'Core Technical Logic';
    elements.hudQText.textContent = question.questionText;

    const percent = Math.min(100, Math.round((qNum / state.totalQuestions) * 100));
    elements.progressBar.style.width = `${percent}%`;

    const limit = question.timeLimitSeconds || 120;
    startQuestionTimer(limit);

    speakCurrentQuestion();
    startSpeechRecognition();
}

function startQuestionTimer(durationSeconds) {
    clearInterval(state.timerInterval);
    state.totalDuration = durationSeconds;
    state.timeRemaining = durationSeconds;

    updateTimerDisplay();

    state.timerInterval = setInterval(() => {
        state.timeRemaining--;
        updateTimerDisplay();

        if (state.timeRemaining <= 0) {
            clearInterval(state.timerInterval);
            console.warn(`Timer expired for Question #${state.currentQuestionNumber}. Auto-submitting...`);
            submitCurrentTurn(true);
        }
    }, 1000);
}

function updateTimerDisplay() {
    const mins = Math.floor(state.timeRemaining / 60);
    const secs = state.timeRemaining % 60;
    elements.hudTimerText.textContent = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;

    const totalCircumference = 188.4;
    const progressOffset = totalCircumference - (state.timeRemaining / state.totalDuration) * totalCircumference;
    elements.timerRingProgress.style.strokeDashoffset = progressOffset;

    elements.timerWidget.classList.remove('warning', 'critical');
    if (state.timeRemaining <= 15) {
        elements.timerWidget.classList.add('critical');
    } else if (state.timeRemaining <= 40) {
        elements.timerWidget.classList.add('warning');
    }
}

function stopSpeechSynthesis() {
    if ('speechSynthesis' in window) {
        window.speechSynthesis.cancel();
    }
}

function speakCurrentQuestion() {
    if (!state.currentQuestion || !state.currentQuestion.questionText) return;

    if ('speechSynthesis' in window) {
        stopSpeechSynthesis();
        const utterance = new SpeechSynthesisUtterance(state.currentQuestion.questionText);
        utterance.rate = 1.0;
        utterance.pitch = 1.0;

        const voices = window.speechSynthesis.getVoices();
        const naturalVoice = voices.find(v => v.lang && v.lang.startsWith('en') && 
            (v.name.includes('Natural') || v.name.includes('Google') || v.name.includes('Samantha') || v.name.includes('David') || v.name.includes('Jenny') || v.name.includes('Guy')));
        if (naturalVoice) utterance.voice = naturalVoice;

        utterance.onerror = (e) => {
            console.debug('TTS utterance event:', e.error);
        };

        setTimeout(() => {
            window.speechSynthesis.speak(utterance);
        }, 50);
    }
}

// ----------------------------------------------------
// SUBMISSION & ADVANCING TO NEXT QUESTION
// ----------------------------------------------------
async function submitCurrentTurn(isTimeout = false) {
    stopSpeechSynthesis();
    clearInterval(state.timerInterval);
    stopSpeechRecognition();

    let transcript = elements.liveTranscript.value.trim();
    if (!transcript && isTimeout) {
        transcript = "Time expired before candidate provided an answer.";
    }

    elements.btnSubmitTurn.disabled = true;
    elements.btnSubmitLabel.textContent = isTimeout ? 'Time Expired — Loading Next...' : 'Evaluating & Loading Next...';

    const timeTaken = state.totalDuration - state.timeRemaining;

    try {
        const response = await fetch(`${API_BASE}/api/interview/next`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                sessionId: state.sessionId,
                questionNumber: state.currentQuestionNumber,
                candidateTranscript: transcript,
                isTimeout: isTimeout,
                timeTakenSeconds: timeTaken
            })
        });

        const turnResult = await response.json();

        if (turnResult.isSessionCompleted || !turnResult.nextQuestion || state.currentQuestionNumber >= state.totalQuestions) {
            // Finished all questions before 15 minutes! Evaluate and show final scorecard
            loadFinalScorecard("Completed All Questions Before 15-Minute Limit");
        } else {
            renderQuestion(turnResult.nextQuestion, state.currentQuestionNumber + 1);
        }
    } catch (err) {
        console.error('Error advancing turn:', err);
        alert('Failed to submit answer: ' + err.message);
    } finally {
        elements.btnSubmitTurn.disabled = false;
        elements.btnSubmitLabel.textContent = 'Submit Answer & Next Question';
    }
}

// ----------------------------------------------------
// STEP 3: FINAL EVALUATED SCORECARD
// ----------------------------------------------------
async function loadFinalScorecard(completionNote = "15-Minute Assessment Completed") {
    stopSpeechSynthesis();
    clearInterval(state.timerInterval);
    clearInterval(state.globalExamTimerInterval);
    stopSpeechRecognition();
    stopLocalMedia();
    stopProctorEngine();

    const elapsedSeconds = state.globalExamDuration - Math.max(0, state.globalExamTimeRemaining);
    const elapsedMins = Math.floor(elapsedSeconds / 60);
    const elapsedSecs = elapsedSeconds % 60;
    const timeSpentStr = `${elapsedMins}m ${elapsedSecs}s / 15m Allocated`;

    try {
        const res = await fetch(`${API_BASE}/api/interview/summary/${state.sessionId}`);
        const scorecard = await res.json();

        elements.reportCandidateName.textContent = `${scorecard.candidateName} — Evaluation Summary (${timeSpentStr})`;
        elements.scoreOverall.textContent = scorecard.overallScore.toFixed(1);
        elements.scoreTech.textContent = scorecard.technicalAverage.toFixed(1);
        elements.scoreComm.textContent = scorecard.communicationAverage.toFixed(1);

        elements.barOverall.style.width = `${Math.min(100, scorecard.overallScore * 10)}%`;
        elements.barTech.style.width = `${Math.min(100, scorecard.technicalAverage * 10)}%`;
        elements.barComm.style.width = `${Math.min(100, scorecard.communicationAverage * 10)}%`;

        elements.reportVerdictText.textContent = scorecard.overallVerdict;
        if (scorecard.overallVerdict.includes('NEEDS IMPROVEMENT')) {
            elements.reportVerdictBadge.className = 'verdict-badge needs-improvement';
        } else {
            elements.reportVerdictBadge.className = 'verdict-badge';
        }

        elements.reportDetailedFeedback.textContent = scorecard.detailedFeedback;

        // Render question turns list
        elements.reportTurnsList.innerHTML = '';
        if (scorecard.turnSummaries && scorecard.turnSummaries.length > 0) {
            scorecard.turnSummaries.forEach(t => {
                const card = document.createElement('div');
                card.className = 'turn-item-card';
                card.innerHTML = `
                    <div class="turn-header">
                        <span class="turn-q-title">Q${t.questionNumber}: ${escapeHtml(t.questionText)}</span>
                        <div class="turn-scores">
                            <span class="score-chip tech">Tech: ${t.technicalScore != null ? t.technicalScore.toFixed(1) : 'N/A'}/10</span>
                            <span class="score-chip comm">Comm: ${t.communicationScore != null ? t.communicationScore.toFixed(1) : 'N/A'}/10</span>
                        </div>
                    </div>
                    <div class="turn-feedback">${escapeHtml(t.feedbackSummary || 'Evaluated')}</div>
                `;
                elements.reportTurnsList.appendChild(card);
            });
        }

        switchStep('step-scorecard');

        // Clear active session tokens so single attempt cannot be re-entered
        sessionStorage.removeItem('active_session_token');
        sessionStorage.removeItem('active_assignment_id');
        state.candidateSchedule.sessionToken = null;
        state.candidateSchedule.activeAssignment = null;
        if (state.supabase.user && state.supabase.user.email) {
            loadCandidateAssignedInterviews(state.supabase.user.email);
        }
    } catch (err) {
        console.error('Failed to load scorecard:', err);
    }
}

// ----------------------------------------------------
// STEP 4: MALPRACTICE DISQUALIFICATION
// ----------------------------------------------------
function disqualifyForMalpractice(reason) {
    state.proctor.isDisqualified = true;
    stopSpeechSynthesis();
    clearInterval(state.timerInterval);
    clearInterval(state.globalExamTimerInterval);
    stopSpeechRecognition();
    stopLocalMedia();
    stopProctorEngine();

    // Clear active session resilience tokens to prevent resuming disqualified attempt
    sessionStorage.removeItem('active_session_token');
    sessionStorage.removeItem('active_assignment_id');
    state.candidateSchedule.sessionToken = null;
    state.candidateSchedule.activeAssignment = null;

    if (elements.fullscreenLockoutOverlay) elements.fullscreenLockoutOverlay.style.display = 'none';
    if (elements.proctorAlertToast) elements.proctorAlertToast.style.display = 'none';

    elements.malpracticeCandidateTitle.textContent = `${state.candidateName} — Disqualification Notice`;
    elements.malpracticeReasonText.textContent = reason || "The interview was automatically terminated because the proctoring threshold (3 violations) was exceeded.";
    elements.malpracticeTotalWarnings.textContent = `${state.proctor.warningsCount} / ${state.proctor.maxWarnings}`;
    elements.malpracticeTimestamp.textContent = new Date().toLocaleTimeString();

    renderMalpracticeAuditTable();
    switchStep('step-malpractice');
}

function renderMalpracticeAuditTable() {
    const tbody = elements.malpracticeAuditTbody;
    tbody.innerHTML = '';

    if (state.proctor.violations.length === 0) {
        tbody.innerHTML = `<tr><td colspan="4" style="text-align:center; color:#94a3b8;">No incident details available</td></tr>`;
        return;
    }

    state.proctor.violations.forEach((v, idx) => {
        const row = document.createElement('tr');
        
        let pillClass = 'tab-switch';
        let label = 'Tab Switch';
        if (v.type === 'MOBILE_DEVICE') {
            pillClass = 'mobile-device';
            label = 'Mobile / Device';
        } else if (v.type === 'FULLSCREEN_EXIT') {
            pillClass = 'fullscreen';
            label = 'Fullscreen Exit';
        } else if (v.type === 'MULTIPLE_FACES') {
            pillClass = 'mobile-device';
            label = 'Multiple People';
        }

        row.innerHTML = `
            <td>#${idx + 1}</td>
            <td><span class="violation-pill ${pillClass}">${label}</span></td>
            <td>${escapeHtml(v.details)}</td>
            <td style="font-family: var(--font-mono); font-size: 0.8rem; color: #94a3b8;">${v.timestamp}</td>
        `;
        tbody.appendChild(row);
    });
}

// ----------------------------------------------------
// PROCTORING ENGINE & COCO-SSD OBJECT DETECTION
// ----------------------------------------------------
async function preloadCocoSsdModel() {
    if (window.cocoSsd && !state.proctor.cocoModel && !state.proctor.isModelLoading) {
        state.proctor.isModelLoading = true;
        try {
            console.log("Loading TensorFlow.js COCO-SSD model in background...");
            state.proctor.cocoModel = await cocoSsd.load({ base: 'lite_mobilenet_v2' });
            console.log("COCO-SSD model ready for real-time mobile detection.");
            if (elements.proctorCamStatusText) {
                elements.proctorCamStatusText.textContent = "AI Proctoring: Active (TensorFlow.js)";
            }
        } catch (e) {
            console.warn("Failed to load COCO-SSD model:", e);
        } finally {
            state.proctor.isModelLoading = false;
        }
    }
}

function startProctorEngine() {
    state.proctor.active = true;
    state.proctor.warningsCount = 0;
    state.proctor.violations = [];
    state.proctor.isDisqualified = false;

    if (elements.proctorStatusHud) {
        elements.proctorStatusHud.style.display = 'flex';
        updateProctorHudBadge();
    }

    startObjectDetectionLoop();
}

function stopProctorEngine() {
    state.proctor.active = false;
    if (state.proctor.detectionInterval) {
        clearInterval(state.proctor.detectionInterval);
        state.proctor.detectionInterval = null;
    }
    if (elements.proctorStatusHud) {
        elements.proctorStatusHud.style.display = 'none';
    }
    clearProctorCanvas();
}

async function requestFullscreenLock() {
    try {
        if (!document.fullscreenElement) {
            await document.documentElement.requestFullscreen();
        }
    } catch (err) {
        console.warn("Fullscreen request error:", err);
    }
}

async function resumeFullscreenMode() {
    if (elements.fullscreenLockoutOverlay) {
        elements.fullscreenLockoutOverlay.style.display = 'none';
    }
    await requestFullscreenLock();
}

function setupProctorEventListeners() {
    // 1. Fullscreen change listener
    document.addEventListener('fullscreenchange', () => {
        if (!state.proctor.active || state.proctor.isDisqualified) return;

        if (!document.fullscreenElement) {
            if (elements.fullscreenLockoutOverlay) {
                elements.fullscreenLockoutOverlay.style.display = 'flex';
            }
            recordProctorViolation('FULLSCREEN_EXIT', 'Candidate exited full-screen proctored mode.');
        } else {
            if (elements.fullscreenLockoutOverlay) {
                elements.fullscreenLockoutOverlay.style.display = 'none';
            }
        }
    });

    // 2. Tab switch (Visibility API)
    document.addEventListener('visibilitychange', () => {
        if (!state.proctor.active || state.proctor.isDisqualified) return;

        if (document.hidden) {
            recordProctorViolation('TAB_SWITCH', 'Candidate switched tabs or minimized the interview browser.');
        }
    });

    // 3. Window blur / focus loss (Dual screen or external app click)
    window.addEventListener('blur', () => {
        if (!state.proctor.active || state.proctor.isDisqualified) return;
        const now = Date.now();
        if (now - state.proctor.lastViolationTime > 1500) {
            recordProctorViolation('TAB_SWITCH', 'Browser window lost focus to an external application.');
        }
    });

    // 4. Anti-Cheat: Disable Right Click & Developer Shortcuts
    document.addEventListener('contextmenu', (e) => {
        if (state.proctor.active) {
            e.preventDefault();
        }
    });

    document.addEventListener('keydown', (e) => {
        if (!state.proctor.active) return;
        if (e.key === 'F12' || 
            (e.ctrlKey && e.shiftKey && (e.key === 'I' || e.key === 'J' || e.key === 'C')) || 
            (e.ctrlKey && e.key === 'u')) {
            e.preventDefault();
            recordProctorViolation('FULLSCREEN_EXIT', 'Attempted to open browser developer tools or view source.');
        }
    });
}

function startObjectDetectionLoop() {
    if (state.proctor.detectionInterval) {
        clearInterval(state.proctor.detectionInterval);
    }

    state.proctor.detectionInterval = setInterval(async () => {
        if (!state.proctor.active || state.proctor.isDisqualified) return;
        if (!state.proctor.cocoModel) {
            await preloadCocoSsdModel();
            return;
        }

        const video = elements.localVideo;
        if (!video || video.readyState < 2) return;

        try {
            const predictions = await state.proctor.cocoModel.detect(video);
            processCocoPredictions(predictions);
        } catch (err) {
            console.debug("Detection cycle skip:", err);
        }
    }, 350);
}

function processCocoPredictions(predictions) {
    const canvas = elements.proctorCanvas;
    const video = elements.localVideo;
    if (!canvas || !video) return;

    const ctx = canvas.getContext('2d');
    canvas.width = video.videoWidth || 640;
    canvas.height = video.videoHeight || 480;

    ctx.clearRect(0, 0, canvas.width, canvas.height);

    let mobileDetected = false;
    let personCount = 0;

    predictions.forEach(p => {
        const [x, y, width, height] = p.bbox;
        const className = p.class.toLowerCase();
        const score = Math.round(p.score * 100);

        if ((className === 'cell phone' || className === 'remote') && p.score > 0.45) {
            mobileDetected = true;

            ctx.strokeStyle = '#ef4444';
            ctx.lineWidth = 4;
            ctx.fillStyle = 'rgba(239, 68, 68, 0.2)';
            ctx.strokeRect(x, y, width, height);
            ctx.fillRect(x, y, width, height);

            ctx.fillStyle = '#ef4444';
            ctx.fillRect(x, y > 26 ? y - 26 : y, width, 26);
            ctx.fillStyle = '#ffffff';
            ctx.font = 'bold 13px Plus Jakarta Sans, sans-serif';
            ctx.fillText(`📱 MOBILE PHONE DETECTED (${score}%)`, x + 6, y > 26 ? y - 8 : y + 18);
        }

        if (className === 'person') {
            personCount++;
        }
    });

    if (mobileDetected) {
        if (elements.proctorCamTag) elements.proctorCamTag.classList.add('alert');
        if (elements.proctorCamStatusText) elements.proctorCamStatusText.textContent = "⚠️ Mobile Device in View!";

        const now = Date.now();
        if (now - state.proctor.lastMobileAlertTime > 4000) {
            state.proctor.lastMobileAlertTime = now;
            recordProctorViolation('MOBILE_DEVICE', 'Mobile phone or unauthorized object detected in camera stream.');
        }
    } else if (personCount > 1) {
        if (elements.proctorCamTag) elements.proctorCamTag.classList.add('alert');
        if (elements.proctorCamStatusText) elements.proctorCamStatusText.textContent = "⚠️ Multiple Persons in View!";
        
        const now = Date.now();
        if (now - state.proctor.lastMobileAlertTime > 5000) {
            state.proctor.lastMobileAlertTime = now;
            recordProctorViolation('MULTIPLE_FACES', 'Multiple people detected in candidate camera frame.');
        }
    } else {
        if (elements.proctorCamTag) elements.proctorCamTag.classList.remove('alert');
        if (elements.proctorCamStatusText) elements.proctorCamStatusText.textContent = "AI Proctoring: Monitoring";
    }
}

function clearProctorCanvas() {
    const canvas = elements.proctorCanvas;
    if (canvas) {
        const ctx = canvas.getContext('2d');
        ctx.clearRect(0, 0, canvas.width, canvas.height);
    }
}

// ----------------------------------------------------
// PROCTOR VIOLATIONS & BACKEND PERSISTENCE
// ----------------------------------------------------
async function recordProctorViolation(type, details) {
    if (!state.proctor.active || state.proctor.isDisqualified) return;

    const now = Date.now();
    if (now - state.proctor.lastViolationTime < 1500) return;
    state.proctor.lastViolationTime = now;

    state.proctor.warningsCount++;
    const currentWarning = state.proctor.warningsCount;
    const timeString = new Date().toLocaleTimeString();

    const incident = {
        type: type,
        details: details,
        warningNumber: currentWarning,
        timestamp: timeString
    };
    state.proctor.violations.push(incident);

    playWarningBeep();
    showProctorToast(type, details, currentWarning);
    updateProctorHudBadge();

    const isDisqualified = currentWarning >= state.proctor.maxWarnings;

    if (state.sessionId) {
        try {
            await fetch(`${API_BASE}/api/interview/session/${state.sessionId}/proctor-violation`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    violationType: type,
                    details: details,
                    warningNumber: currentWarning,
                    isMalpractice: isDisqualified,
                    timestamp: new Date().toISOString()
                })
            });
        } catch (err) {
            console.error("Failed to sync proctor violation with backend:", err);
        }
    }

    if (isDisqualified) {
        disqualifyForMalpractice(details);
    }
}

function updateProctorHudBadge() {
    if (!elements.proctorWarningsBadge || !elements.proctorStatusHud) return;

    elements.proctorWarningsBadge.textContent = `Violations: ${state.proctor.warningsCount}/${state.proctor.maxWarnings}`;
    elements.proctorStatusHud.classList.remove('warning-level-1', 'warning-level-2');

    if (state.proctor.warningsCount === 1) {
        elements.proctorStatusHud.classList.add('warning-level-1');
    } else if (state.proctor.warningsCount >= 2) {
        elements.proctorStatusHud.classList.add('warning-level-2');
    }
}

function showProctorToast(type, message, warningNum) {
    const toast = elements.proctorAlertToast;
    if (!toast) return;

    let title = "⚠️ Warning: Policy Violation Detected";
    if (type === 'TAB_SWITCH') title = "⚠️ Warning: Tab Switch Detected";
    if (type === 'MOBILE_DEVICE') title = "📱 Warning: Mobile Phone Detected";
    if (type === 'FULLSCREEN_EXIT') title = "🔒 Warning: Fullscreen Mode Exited";
    if (type === 'MULTIPLE_FACES') title = "👥 Warning: Multiple Persons in Frame";

    elements.proctorToastTitle.textContent = title;
    elements.proctorToastMessage.textContent = `${message} Please maintain exam integrity.`;
    elements.proctorToastCount.textContent = `Warning ${warningNum} of ${state.proctor.maxWarnings}`;

    toast.style.display = 'block';

    if (state.proctor.toastTimeout) clearTimeout(state.proctor.toastTimeout);
    state.proctor.toastTimeout = setTimeout(() => {
        toast.style.display = 'none';
    }, 4500);
}

function playWarningBeep() {
    try {
        const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();

        osc.type = 'sawtooth';
        osc.frequency.setValueAtTime(440, audioCtx.currentTime);
        osc.frequency.setValueAtTime(220, audioCtx.currentTime + 0.15);

        gain.gain.setValueAtTime(0.15, audioCtx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.35);

        osc.connect(gain);
        gain.connect(audioCtx.destination);

        osc.start();
        osc.stop(audioCtx.currentTime + 0.35);
    } catch (e) {
        console.debug("AudioContext alert tone skipped:", e);
    }
}

// ----------------------------------------------------
// AUDIO WAVEFORM & WEBRTC MEDIA HELPERS
// ----------------------------------------------------
async function initLocalMedia() {
    try {
        state.mediaStream = await navigator.mediaDevices.getUserMedia({
            video: true,
            audio: true
        });
        elements.localVideo.srcObject = state.mediaStream;

        state.audioContext = new (window.AudioContext || window.webkitAudioContext)();
        const source = state.audioContext.createMediaStreamSource(state.mediaStream);
        state.analyser = state.audioContext.createAnalyser();
        state.analyser.fftSize = 64;
        source.connect(state.analyser);

        drawWaveform();
    } catch (err) {
        console.warn('Camera/Microphone access not granted or unavailable:', err);
    }
}

function drawWaveform() {
    if (!state.analyser) return;
    const canvas = elements.waveformCanvas;
    const ctx = canvas.getContext('2d');
    const bufferLength = state.analyser.frequencyBinCount;
    const dataArray = new Uint8Array(bufferLength);

    function render() {
        requestAnimationFrame(render);
        state.analyser.getByteFrequencyData(dataArray);

        ctx.clearRect(0, 0, canvas.width, canvas.height);
        const barWidth = (canvas.width / bufferLength) * 2;
        let x = 0;

        for (let i = 0; i < bufferLength; i++) {
            const barHeight = (dataArray[i] / 255) * canvas.height;
            const gradient = ctx.createLinearGradient(0, canvas.height, 0, 0);
            gradient.addColorStop(0, '#6366f1');
            gradient.addColorStop(1, '#a855f7');

            ctx.fillStyle = gradient;
            ctx.fillRect(x, canvas.height - barHeight, barWidth - 2, barHeight);
            x += barWidth;
        }
    }
    render();
}

function stopLocalMedia() {
    if (state.mediaStream) {
        state.mediaStream.getTracks().forEach(t => t.stop());
    }
}

function initLiveKit(livekitConfig) {
    elements.livekitRoomTag.textContent = `Room: ${livekitConfig.roomName}`;
    elements.livekitStatusPill.textContent = 'Active Room';
}

// ----------------------------------------------------
// SPEECH RECOGNITION (STT) HELPER
// ----------------------------------------------------
function setupSpeechRecognition() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
        console.warn('Web Speech API is not supported in this browser.');
        return;
    }

    try {
        state.speechRecognition = new SpeechRecognition();
        state.speechRecognition.continuous = true;
        state.speechRecognition.interimResults = true;
        state.speechRecognition.lang = 'en-US';

        state.speechRecognition.onresult = (event) => {
            let interim = '';
            for (let i = event.resultIndex; i < event.results.length; ++i) {
                const chunk = event.results[i][0].transcript;
                if (event.results[i].isFinal) {
                    state.finalTranscript += (state.finalTranscript ? ' ' : '') + chunk.trim();
                } else {
                    interim += chunk;
                }
            }

            const combined = (state.finalTranscript + (interim ? ' ' + interim : '')).trim();
            if (combined) {
                elements.liveTranscript.value = combined;
                elements.liveTranscript.scrollTop = elements.liveTranscript.scrollHeight;
            }
        };

        state.speechRecognition.onend = () => {
            if (state.isRecognizing) {
                try {
                    state.speechRecognition.start();
                } catch (err) {
                    setTimeout(() => {
                        if (state.isRecognizing) {
                            try { state.speechRecognition.start(); } catch (e) {}
                        }
                    }, 200);
                }
            }
        };

        state.speechRecognition.onerror = (e) => {
            console.warn('Speech recognition error:', e.error);
            if (e.error === 'not-allowed' || e.error === 'service-not-allowed') {
                state.isRecognizing = false;
            }
        };

        elements.liveTranscript.addEventListener('input', () => {
            state.finalTranscript = elements.liveTranscript.value;
        });

    } catch (e) {
        console.error('Failed to setup SpeechRecognition:', e);
    }
}

function startSpeechRecognition() {
    state.finalTranscript = '';
    elements.liveTranscript.value = '';
    state.isRecognizing = true;

    if (state.speechRecognition) {
        try {
            state.speechRecognition.start();
        } catch (e) {
            console.debug('Speech recognition already active or pending:', e.message);
        }
    }
}

function stopSpeechRecognition() {
    state.isRecognizing = false;
    if (state.speechRecognition) {
        try {
            state.speechRecognition.stop();
        } catch (e) {}
    }
}

// ----------------------------------------------------
// UI NAVIGATION & UTILITY HELPERS
// ----------------------------------------------------
function switchStep(stepId) {
    [elements.stepLogin, elements.stepSetup, elements.stepInterview, elements.stepScorecard, elements.stepMalpractice].forEach(el => {
        if (el) el.classList.remove('active');
    });
    const target = document.getElementById(stepId);
    if (target) {
        target.classList.add('active');
    }
    // Hide header action buttons on login page
    const headerActions = document.querySelector('.header-actions');
    if (headerActions) {
        headerActions.style.display = stepId === 'step-login' ? 'none' : 'flex';
    }
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function resetToSetup() {
    stopSpeechSynthesis();
    stopSpeechRecognition();
    clearInterval(state.timerInterval);
    clearInterval(state.globalExamTimerInterval);
    stopLocalMedia();
    stopProctorEngine();
    if (document.fullscreenElement) {
        document.exitFullscreen().catch(() => {});
    }

    // Reset scheduled interview state & clear resilience tokens
    sessionStorage.removeItem('active_session_token');
    sessionStorage.removeItem('active_assignment_id');
    state.candidateSchedule.sessionToken = null;
    state.candidateSchedule.activeAssignment = null;
    if (elements.activeDrivePillBox) elements.activeDrivePillBox.style.display = 'none';

    // If user is not logged in, always return to login page
    if (!state.supabase.user) {
        switchStep('step-login');
        return;
    }

    // Refresh candidate assigned interviews if user is signed in
    if (state.supabase.user.email) {
        loadCandidateAssignedInterviews(state.supabase.user.email);
    }

    switchStep('step-setup');
}

function setButtonLoading(loading, text) {
    const btn = document.getElementById('btn-start-interview');
    if (btn) {
        btn.disabled = loading;
        btn.querySelector('span').textContent = text;
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// ==========================================================================
// SUPABASE AUTHENTICATION & CONFIGURATION ENGINE
// ==========================================================================

async function initSupabaseAuth() {
    try {
        console.log("Initializing Supabase Auth engine...");

        // 1. Fetch server-side configuration
        let serverConfig = { supabaseUrl: "https://eiwqkbnhdzbtqjvlvxji.supabase.co", supabaseAnonKey: "", isConfigured: false };
        try {
            const res = await fetch(`${API_BASE}/api/auth/config`);
            if (res.ok) {
                serverConfig = await res.json();
            }
        } catch (e) {
            console.warn("Could not fetch server auth config, falling back to local defaults:", e);
        }

        // 2. Local storage overrides (allows user to enter key anytime in the UI)
        const localKey = localStorage.getItem('supabase_anon_key');
        const localUrl = localStorage.getItem('supabase_url');

        const activeUrl = localUrl || serverConfig.supabaseUrl || "https://eiwqkbnhdzbtqjvlvxji.supabase.co";
        const activeKey = localKey || serverConfig.supabaseAnonKey || "";

        state.supabase.url = activeUrl;
        state.supabase.anonKey = activeKey;

        // Pre-fill config form inputs if available
        if (elements.sbInputUrl) elements.sbInputUrl.value = activeUrl;
        if (elements.sbInputAnonKey && activeKey) elements.sbInputAnonKey.value = activeKey;

        // 3. Check if valid key exists
        const hasValidKey = activeKey && activeKey.trim().length > 20 && !activeKey.includes("YOUR_SUPABASE_ANON_KEY");

        if (hasValidKey && window.supabase) {
            state.supabase.isConfigured = true;
            state.supabase.client = window.supabase.createClient(activeUrl, activeKey);

            // Update UI status pill
            updateSupabaseStatusUI(true);

            // Register Auth state listener
            state.supabase.client.auth.onAuthStateChange((event, session) => {
                console.log("Supabase Auth event:", event, session ? session.user.email : "no session");
                handleAuthStateChange(event, session);
            });

            // Check current active session
            const { data, error } = await state.supabase.client.auth.getSession();
            if (!error && data && data.session) {
                handleAuthStateChange('INITIAL_SESSION', data.session);
            } else {
                restoreSavedDirectSession();
            }
        } else {
            state.supabase.isConfigured = false;
            updateSupabaseStatusUI(false);
            restoreSavedDirectSession();
            console.info("Supabase client awaiting Anon Key. Click 'Supabase: Key Needed' in header to enter it.");
        }
    } catch (err) {
        console.error("Failed to initialize Supabase Auth:", err);
        updateSupabaseStatusUI(false);
        restoreSavedDirectSession();
    }
}

function restoreSavedDirectSession() {
    const savedUserStr = localStorage.getItem('auth_user');
    if (savedUserStr) {
        try {
            const savedUser = JSON.parse(savedUserStr);
            const role = localStorage.getItem('user_role') || 'STUDENT';
            const pseudoUser = {
                id: savedUser.id || 'auth-user',
                email: savedUser.email || (role === 'ADMIN' ? 'admin@interview.ai' : 'alex.morgan@testcampus.edu'),
                user_metadata: {
                    full_name: savedUser.name || (role === 'ADMIN' ? 'Administrator' : 'Alex Morgan'),
                    role: role.toLowerCase(),
                    roll_no: savedUser.rollNo || (role === 'STUDENT' ? '24AM119' : null)
                }
            };
            state.authRole = role;
            state.supabase.user = pseudoUser;
            updateAuthUI(pseudoUser);
            if (role === 'STUDENT' && savedUser.email) {
                loadCandidateAssignedInterviews(savedUser.email);
            }
            return true;
        } catch (e) {
            console.warn("Could not restore saved direct session:", e);
        }
    }
    updateAuthUI(null);
    return false;
}

function updateSupabaseStatusUI(isConnected) {
    if (!elements.sbStatusDot || !elements.sbStatusText) return;

    if (isConnected) {
        elements.sbStatusDot.classList.remove('pending');
        elements.sbStatusText.textContent = "Supabase: Ready";
        if (elements.authMissingKeyBanner) elements.authMissingKeyBanner.style.display = 'none';
    } else {
        elements.sbStatusDot.classList.add('pending');
        elements.sbStatusText.textContent = "Supabase: Key Needed";
        if (elements.authMissingKeyBanner) elements.authMissingKeyBanner.style.display = 'flex';
    }
}

function getResolvedRole(user) {
    if (!user) return 'STUDENT';
    const email = (user.email || '').toLowerCase();
    const userRoleMeta = user.user_metadata?.role;
    if (email.includes('admin') || userRoleMeta === 'admin' || state.authRole === 'ADMIN') {
        return 'ADMIN';
    }
    return 'STUDENT';
}

function handleAuthStateChange(event, session) {
    if (session && session.user) {
        state.supabase.session = session;
        state.supabase.user = session.user;

        const role = getResolvedRole(session.user);
        state.authRole = role;
        localStorage.setItem('user_role', role);

        updateAuthUI(session.user);

        // Auto-fill Candidate Name from user metadata if name is still default or empty
        const metaName = session.user.user_metadata?.full_name || session.user.user_metadata?.name;
        if (metaName && elements.candidateNameInput) {
            elements.candidateNameInput.value = metaName;
            state.candidateName = metaName;
        } else if (session.user.email && (!elements.candidateNameInput.value || elements.candidateNameInput.value === 'Jane Doe')) {
            const derivedName = session.user.email.split('@')[0];
            elements.candidateNameInput.value = derivedName.charAt(0).toUpperCase() + derivedName.slice(1);
            state.candidateName = elements.candidateNameInput.value;
        }

        // Role-specific views
        if (role === 'STUDENT' && session.user.email) {
            loadCandidateAssignedInterviews(session.user.email);
        } else if (role === 'ADMIN') {
            if (elements.candidateAssignedSection) {
                elements.candidateAssignedSection.style.display = 'none';
            }
        }

        if (elements.btnOpenChangePw) {
            elements.btnOpenChangePw.style.display = 'inline-flex';
        }
    } else {
        state.supabase.session = null;
        state.supabase.user = null;
        updateAuthUI(null);

        if (elements.candidateAssignedSection) {
            elements.candidateAssignedSection.style.display = 'none';
        }
        if (elements.btnOpenChangePw) {
            elements.btnOpenChangePw.style.display = 'none';
        }
        if (elements.activeDrivePillBox) {
            elements.activeDrivePillBox.style.display = 'none';
        }
        if (elements.adminWelcomeBanner) {
            elements.adminWelcomeBanner.style.display = 'none';
        }
        state.candidateSchedule.activeAssignment = null;
    }
}

function updateAuthUI(user) {
    if (user) {
        if (elements.authUnauthenticated) elements.authUnauthenticated.style.display = 'none';
        if (elements.authAuthenticated) elements.authAuthenticated.style.display = 'inline-flex';

        const role = getResolvedRole(user);
        state.authRole = role;

        const rawName = user.user_metadata?.full_name || user.email?.split('@')[0] || (role === 'ADMIN' ? 'Administrator' : 'Candidate');
        const rollNo = user.user_metadata?.roll_no;
        const displayName = (rollNo && role === 'STUDENT') ? `${rawName} (${rollNo})` : rawName;
        const email = user.email || "";

        if (elements.userDisplayName) elements.userDisplayName.textContent = displayName;
        if (elements.userDisplayEmail) elements.userDisplayEmail.textContent = email;
        if (elements.userAvatarPill) {
            elements.userAvatarPill.textContent = role === 'ADMIN' ? '👑' : displayName.charAt(0).toUpperCase();
        }

        // Prominent Role Badge in Header
        if (elements.userRoleBadge) {
            elements.userRoleBadge.className = `user-role-pill ${role === 'ADMIN' ? 'role-admin' : 'role-student'}`;
            elements.userRoleBadge.style.display = 'inline-flex';
        }
        if (elements.userRoleIcon) {
            elements.userRoleIcon.textContent = role === 'ADMIN' ? '👑' : '🎓';
        }
        if (elements.userRoleText) {
            elements.userRoleText.textContent = role === 'ADMIN' ? 'ADMIN' : 'STUDENT';
        }

        // Conditional display based on role
        if (role === 'ADMIN') {
            if (elements.adminWelcomeBanner) elements.adminWelcomeBanner.style.display = 'block';
            if (elements.candidateAssignedSection) elements.candidateAssignedSection.style.display = 'none';
            if (elements.candidateForm) elements.candidateForm.style.display = 'none';
            if (elements.practiceInterviewCard) elements.practiceInterviewCard.style.display = 'none';
            if (elements.resumeGateCard) elements.resumeGateCard.style.display = 'none';
            if (elements.activeDrivePillBox) elements.activeDrivePillBox.style.display = 'none';
            // Move admin modal content inline
            const heroCard = document.querySelector('.hero-card.setup-split-layout');
            if (heroCard) heroCard.style.display = 'none';
            mountAdminInline();
            loadAdminSchedules();
        } else {
            if (elements.adminWelcomeBanner) elements.adminWelcomeBanner.style.display = 'none';
            if (elements.candidateForm) elements.candidateForm.style.display = '';
            if (elements.practiceInterviewCard) elements.practiceInterviewCard.style.display = '';
            const heroCard = document.querySelector('.hero-card.setup-split-layout');
            if (heroCard) heroCard.style.display = '';
            unmountAdminInline();
        }
    } else {
        if (elements.authUnauthenticated) elements.authUnauthenticated.style.display = 'inline-flex';
        if (elements.authAuthenticated) elements.authAuthenticated.style.display = 'none';
        if (elements.adminWelcomeBanner) elements.adminWelcomeBanner.style.display = 'none';
        if (elements.candidateAssignedSection) elements.candidateAssignedSection.style.display = 'none';
    }
}

function setupSupabaseEventListeners() {
    // Open/Close Modals
    if (elements.btnSupabaseConfig) {
        elements.btnSupabaseConfig.addEventListener('click', () => {
            elements.modalSupabaseConfig.style.display = 'flex';
        });
    }

    if (elements.btnOpenAuthModal) {
        elements.btnOpenAuthModal.addEventListener('click', () => {
            elements.modalSupabaseAuth.style.display = 'flex';
        });
    }

    if (elements.btnCloseAuthModal) {
        elements.btnCloseAuthModal.addEventListener('click', () => {
            elements.modalSupabaseAuth.style.display = 'none';
        });
    }

    if (elements.btnCloseConfigModal) {
        elements.btnCloseConfigModal.addEventListener('click', () => {
            elements.modalSupabaseConfig.style.display = 'none';
        });
    }

    if (elements.btnGoToConfig) {
        elements.btnGoToConfig.addEventListener('click', () => {
            elements.modalSupabaseAuth.style.display = 'none';
            elements.modalSupabaseConfig.style.display = 'flex';
        });
    }

    // Close modals on clicking backdrop
    [elements.modalSupabaseAuth, elements.modalSupabaseConfig].forEach(modal => {
        if (modal) {
            modal.addEventListener('click', (e) => {
                if (e.target === modal) modal.style.display = 'none';
            });
        }
    });

    // Dual Role Selection Switcher
    function setModalRole(role) {
        state.authRole = role;
        if (role === 'ADMIN') {
            if (elements.btnRoleAdmin) elements.btnRoleAdmin.classList.add('active');
            if (elements.btnRoleStudent) elements.btnRoleStudent.classList.remove('active');
            if (elements.labelSigninEmail) elements.labelSigninEmail.textContent = 'Administrator Username or Email';
            if (elements.authSigninEmail) elements.authSigninEmail.placeholder = 'admin or admin@interview.ai';
            if (elements.btnSubmitSigninText) elements.btnSubmitSigninText.textContent = 'Sign In as Administrator';
            if (elements.demoCredentialsText) {
                elements.demoCredentialsText.innerHTML = 'Demo Admin: <code>admin</code> (pwd: <code>Admin@2026</code>)';
            }
        } else {
            if (elements.btnRoleStudent) elements.btnRoleStudent.classList.add('active');
            if (elements.btnRoleAdmin) elements.btnRoleAdmin.classList.remove('active');
            if (elements.labelSigninEmail) elements.labelSigninEmail.textContent = 'Student Roll No or Email';
            if (elements.authSigninEmail) elements.authSigninEmail.placeholder = '24AM119 or alex.morgan@testcampus.edu';
            if (elements.btnSubmitSigninText) elements.btnSubmitSigninText.textContent = 'Sign In as Student';
            if (elements.demoCredentialsText) {
                elements.demoCredentialsText.innerHTML = 'Demo Student: <code>24AM119</code> / <code>alex.morgan@testcampus.edu</code> (pwd: <code>Candidate@2026</code>)';
            }
        }
    }

    if (elements.btnRoleStudent) {
        elements.btnRoleStudent.addEventListener('click', () => setModalRole('STUDENT'));
    }
    if (elements.btnRoleAdmin) {
        elements.btnRoleAdmin.addEventListener('click', () => setModalRole('ADMIN'));
    }

    if (elements.btnAutofillDemo) {
        elements.btnAutofillDemo.addEventListener('click', () => {
            if (state.authRole === 'ADMIN') {
                if (elements.authSigninEmail) elements.authSigninEmail.value = 'admin@interview.ai';
                if (elements.authSigninPassword) elements.authSigninPassword.value = 'Admin@2026';
            } else {
                if (elements.authSigninEmail) elements.authSigninEmail.value = 'alex.morgan@testcampus.edu';
                if (elements.authSigninPassword) elements.authSigninPassword.value = 'Candidate@2026';
            }
        });
    }

    // Forgot Password & Reset Listeners
    if (elements.btnShowForgotPw) {
        elements.btnShowForgotPw.addEventListener('click', () => {
            if (elements.formAuthSignin) elements.formAuthSignin.style.display = 'none';
            if (elements.formAuthSignup) elements.formAuthSignup.style.display = 'none';
            if (elements.formAuthMagic) elements.formAuthMagic.style.display = 'none';
            if (elements.formAuthReset) {
                elements.formAuthReset.style.display = 'block';
                if (elements.authSigninEmail && elements.authResetEmail) {
                    elements.authResetEmail.value = elements.authSigninEmail.value;
                }
            }
            if (elements.authResetFeedback) elements.authResetFeedback.style.display = 'none';
        });
    }

    if (elements.btnBackToSignin) {
        elements.btnBackToSignin.addEventListener('click', () => {
            if (elements.formAuthReset) elements.formAuthReset.style.display = 'none';
            if (elements.formAuthSignin) elements.formAuthSignin.style.display = 'flex';
        });
    }

    if (elements.formAuthReset) {
        elements.formAuthReset.addEventListener('submit', async (e) => {
            e.preventDefault();
            const email = elements.authResetEmail.value.trim();
            const newPassword = elements.authResetPassword.value;
            const confirmPassword = elements.authResetConfirm.value;

            if (!email) {
                showAdminFeedback(elements.authResetFeedback, "Please enter your student email.", "error");
                return;
            }
            if (!newPassword || newPassword.length < 6) {
                showAdminFeedback(elements.authResetFeedback, "Password must be at least 6 characters.", "error");
                return;
            }
            if (newPassword !== confirmPassword) {
                showAdminFeedback(elements.authResetFeedback, "Passwords do not match.", "error");
                return;
            }

            try {
                showAdminFeedback(elements.authResetFeedback, "Updating your password...", "info");

                const res = await fetch(`${API_BASE}/api/candidate/schedule/reset-password`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email, newPassword })
                });

                const data = await res.json();
                if (!res.ok || !data.success) {
                    throw new Error(data.message || "Failed to update password.");
                }

                // If Supabase client is available, also update password there
                if (state.supabase.client) {
                    try {
                        if (state.supabase.user && state.supabase.user.email === email) {
                            await state.supabase.client.auth.updateUser({ password: newPassword });
                        }
                    } catch (e) {
                        console.warn("Supabase auth sync skipped:", e);
                    }
                }

                showAdminFeedback(elements.authResetFeedback, "✅ " + data.message, "success");
                setTimeout(() => {
                    if (elements.formAuthReset) elements.formAuthReset.style.display = 'none';
                    if (elements.formAuthSignin) elements.formAuthSignin.style.display = 'flex';
                    if (elements.authSigninEmail) elements.authSigninEmail.value = email;
                    if (elements.authSigninPassword) elements.authSigninPassword.value = newPassword;
                    showAuthFeedback("Password updated successfully! Sign in to continue.", "success");
                }, 1400);
            } catch (err) {
                showAdminFeedback(elements.authResetFeedback, err.message, "error");
            }
        });
    }

    // Auth Navigation Tabs Switcher
    const tabs = [
        { btn: elements.tabBtnSignin, panel: elements.formAuthSignin },
        { btn: elements.tabBtnSignup, panel: elements.formAuthSignup },
        { btn: elements.tabBtnMagic, panel: elements.formAuthMagic }
    ];

    tabs.forEach(({ btn, panel }) => {
        if (btn && panel) {
            btn.addEventListener('click', () => {
                if (elements.formAuthReset) elements.formAuthReset.style.display = 'none';
                tabs.forEach(t => {
                    t.btn.classList.remove('active');
                    t.panel.style.display = 'none';
                });
                btn.classList.add('active');
                panel.style.display = 'flex';
                clearAuthFeedback();
            });
        }
    });

    // Sign In Form Submit
    if (elements.formAuthSignin) {
        elements.formAuthSignin.addEventListener('submit', async (e) => {
            e.preventDefault();

            const identifier = elements.authSigninEmail.value.trim();
            const password = elements.authSigninPassword.value;

            try {
                setAuthButtonLoading(elements.btnSubmitSignin, true, 'Signing in...');
                clearAuthFeedback();

                // 1. Direct Backend Database Authentication Check
                let backendAuthSuccess = false;
                let resolvedUser = null;
                try {
                    const res = await fetch(`${API_BASE}/api/auth/login`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            usernameOrEmail: identifier,
                            password: password,
                            role: state.authRole
                        })
                    });
                    if (res.ok) {
                        const loginData = await res.json();
                        if (loginData.success) {
                            backendAuthSuccess = true;
                            state.authRole = loginData.role || state.authRole;
                            resolvedUser = loginData.user;
                            localStorage.setItem('user_role', state.authRole);
                        }
                    }
                } catch (beErr) {
                    console.info("Direct backend login check bypassed:", beErr.message);
                }

                // 2. Supabase Auth Integration (only if backend auth did not already succeed)
                if (state.supabase.client && !backendAuthSuccess) {
                    let emailForSupabase = identifier.includes('@') ? identifier : (identifier === 'admin' ? 'admin@interview.ai' : 'alex.morgan@testcampus.edu');
                    let { data, error } = await state.supabase.client.auth.signInWithPassword({
                        email: emailForSupabase,
                        password: password
                    });

                    if (error && (identifier.toLowerCase().includes('admin') || identifier.toLowerCase().includes('alex') || identifier.toUpperCase() === '24AM119')) {
                        const fallbackRole = (identifier.toLowerCase().includes('admin') || state.authRole === 'ADMIN') ? 'admin' : 'student';
                        const fallbackName = fallbackRole === 'admin' ? 'Administrator' : 'Alex Morgan';
                        const signUpRes = await state.supabase.client.auth.signUp({
                            email: emailForSupabase,
                            password: password,
                            options: { data: { full_name: fallbackName, role: fallbackRole, roll_no: '24AM119' } }
                        });
                        if (signUpRes.data && signUpRes.data.user) {
                            data = signUpRes.data;
                            error = null;
                        }
                    }

                    if (error) throw error;
                } else if (backendAuthSuccess && resolvedUser) {
                    // Direct database login without Supabase dependency
                    const pseudoUser = {
                        id: resolvedUser.id || 'direct-user',
                        email: resolvedUser.email || identifier,
                        user_metadata: {
                            full_name: resolvedUser.name || (state.authRole === 'ADMIN' ? 'Administrator' : 'Alex Morgan'),
                            role: state.authRole.toLowerCase(),
                            roll_no: resolvedUser.rollNo || (state.authRole === 'STUDENT' ? '24AM119' : null)
                        }
                    };
                    handleAuthStateChange({ user: pseudoUser });
                }

                if (!backendAuthSuccess && !state.supabase.client) {
                    throw new Error("Could not verify credentials. Please check your username/email and password.");
                }

                showAuthFeedback(`Signed in successfully as ${state.authRole}!`, "success");
                setTimeout(() => {
                    elements.modalSupabaseAuth.style.display = 'none';
                    clearAuthFeedback();
                }, 900);
            } catch (err) {
                showAuthFeedback(err.message || "Failed to sign in. Please verify your credentials.", "error");
            } finally {
                setAuthButtonLoading(elements.btnSubmitSignin, false, state.authRole === 'ADMIN' ? 'Sign In as Administrator' : 'Sign In as Student');
            }
        });
    }

    // Sign Up Form Submit
    if (elements.formAuthSignup) {
        elements.formAuthSignup.addEventListener('submit', async (e) => {
            e.preventDefault();
            if (!ensureSupabaseConfigured()) return;

            const name = elements.authSignupName.value.trim();
            const email = elements.authSignupEmail.value.trim();
            const password = elements.authSignupPassword.value;

            try {
                setAuthButtonLoading(elements.btnSubmitSignup, true, 'Creating account...');
                clearAuthFeedback();

                const { data, error } = await state.supabase.client.auth.signUp({
                    email,
                    password,
                    options: { data: { full_name: name } }
                });

                if (error) throw error;

                if (data.session) {
                    showAuthFeedback("Account created & signed in!", "success");
                    setTimeout(() => {
                        elements.modalSupabaseAuth.style.display = 'none';
                        clearAuthFeedback();
                    }, 1200);
                } else {
                    showAuthFeedback("Account created! Please check your email to confirm registration.", "success");
                }
            } catch (err) {
                showAuthFeedback(err.message || "Failed to create account.", "error");
            } finally {
                setAuthButtonLoading(elements.btnSubmitSignup, false, 'Create Account');
            }
        });
    }

    // Magic Link Form Submit
    if (elements.formAuthMagic) {
        elements.formAuthMagic.addEventListener('submit', async (e) => {
            e.preventDefault();
            if (!ensureSupabaseConfigured()) return;

            const email = elements.authMagicEmail.value.trim();

            try {
                setAuthButtonLoading(elements.btnSubmitMagic, true, 'Sending link...');
                clearAuthFeedback();

                const { error } = await state.supabase.client.auth.signInWithOtp({
                    email,
                    options: { emailRedirectTo: window.location.origin }
                });

                if (error) throw error;

                showAuthFeedback("Passwordless login link sent! Check your inbox.", "success");
            } catch (err) {
                showAuthFeedback(err.message || "Could not send magic link.", "error");
            } finally {
                setAuthButtonLoading(elements.btnSubmitMagic, false, 'Send Magic Link');
            }
        });
    }

    // Social OAuth Logins
    if (elements.btnOauthGithub) {
        elements.btnOauthGithub.addEventListener('click', () => handleOAuthLogin('github'));
    }
    if (elements.btnOauthGoogle) {
        elements.btnOauthGoogle.addEventListener('click', () => handleOAuthLogin('google'));
    }

    // Sign Out
    if (elements.btnSignout) {
        elements.btnSignout.addEventListener('click', async () => {
            if (state.supabase.client) {
                try {
                    await state.supabase.client.auth.signOut();
                } catch (e) {
                    console.error("Signout error:", e);
                }
            }
            state.supabase.session = null;
            state.supabase.user = null;
            state.candidateSchedule.activeAssignment = null;
            state.candidateSchedule.sessionToken = null;
            localStorage.removeItem('auth_user');
            localStorage.removeItem('user_role');
            sessionStorage.removeItem('active_session_token');
            sessionStorage.removeItem('active_assignment_id');
            if (elements.activeDrivePillBox) elements.activeDrivePillBox.style.display = 'none';
            if (elements.adminWelcomeBanner) elements.adminWelcomeBanner.style.display = 'none';
            updateAuthUI(null);
            switchStep('step-login');
        });
    }


    // Save Supabase Configuration (URL + Key)
    if (elements.formSupabaseConfig) {
        elements.formSupabaseConfig.addEventListener('submit', async (e) => {
            e.preventDefault();
            const url = elements.sbInputUrl.value.trim();
            const anonKey = elements.sbInputAnonKey.value.trim();

            if (!anonKey) {
                showConfigFeedback("Please paste a valid Supabase anon key.", "error");
                return;
            }

            try {
                showConfigFeedback("Validating key and updating configuration...", "info");

                // Save to localStorage
                localStorage.setItem('supabase_url', url);
                localStorage.setItem('supabase_anon_key', anonKey);

                // Notify Spring Boot backend
                try {
                    await fetch(`${API_BASE}/api/auth/config`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ supabaseUrl: url, supabaseAnonKey: anonKey })
                    });
                } catch (err) {
                    console.warn("Backend runtime config update skipped:", err);
                }

                // Re-initialize client
                await initSupabaseAuth();

                showConfigFeedback("Supabase connected successfully!", "success");
                setTimeout(() => {
                    elements.modalSupabaseConfig.style.display = 'none';
                    if (elements.sbConfigFeedback) elements.sbConfigFeedback.style.display = 'none';
                }, 1200);
            } catch (err) {
                showConfigFeedback("Failed to connect: " + err.message, "error");
            }
        });
    }

    // Clear Stored Key
    if (elements.btnClearSbConfig) {
        elements.btnClearSbConfig.addEventListener('click', () => {
            localStorage.removeItem('supabase_anon_key');
            localStorage.removeItem('supabase_url');
            if (elements.sbInputAnonKey) elements.sbInputAnonKey.value = '';
            initSupabaseAuth();
            showConfigFeedback("Stored credentials cleared.", "info");
        });
    }
}

async function handleOAuthLogin(provider) {
    if (!ensureSupabaseConfigured()) return;
    try {
        const { error } = await state.supabase.client.auth.signInWithOAuth({
            provider: provider,
            options: { redirectTo: window.location.origin }
        });
        if (error) throw error;
    } catch (err) {
        showAuthFeedback(`Could not initiate ${provider} login: ${err.message}`, "error");
    }
}

function ensureSupabaseConfigured() {
    if (!state.supabase.isConfigured || !state.supabase.client) {
        showAuthFeedback("Supabase Anon Key is not yet set. Click 'Set Key Now' above or configure in application.yml.", "error");
        return false;
    }
    return true;
}

function showAuthFeedback(msg, type = "info") {
    if (!elements.authFeedbackBox) return;
    elements.authFeedbackBox.className = `modal-feedback-box ${type}`;
    elements.authFeedbackBox.textContent = msg;
    elements.authFeedbackBox.style.display = 'block';
}

function clearAuthFeedback() {
    if (elements.authFeedbackBox) elements.authFeedbackBox.style.display = 'none';
}

function showConfigFeedback(msg, type = "info") {
    if (!elements.sbConfigFeedback) return;
    elements.sbConfigFeedback.className = `modal-feedback-box ${type}`;
    elements.sbConfigFeedback.textContent = msg;
    elements.sbConfigFeedback.style.display = 'block';
}

function setAuthButtonLoading(btn, isLoading, text) {
    if (!btn) return;
    btn.disabled = isLoading;
    const span = btn.querySelector('span');
    if (span) span.textContent = text;
}

// ----------------------------------------------------
// DEDICATED LOGIN PAGE (ENTRY POINT) ENGINE
// ----------------------------------------------------

function setupLoginPageListeners() {
    // Role Switching Tabs
    if (elements.tabLoginStudent && elements.tabLoginAdmin) {
        elements.tabLoginStudent.addEventListener('click', () => {
            elements.tabLoginStudent.classList.add('active');
            elements.tabLoginAdmin.classList.remove('active');
            if (elements.formLoginPageStudent) elements.formLoginPageStudent.style.display = 'flex';
            if (elements.formLoginPageAdmin) elements.formLoginPageAdmin.style.display = 'none';
            if (elements.loginPageFeedback) elements.loginPageFeedback.style.display = 'none';
        });

        elements.tabLoginAdmin.addEventListener('click', () => {
            elements.tabLoginAdmin.classList.add('active');
            elements.tabLoginStudent.classList.remove('active');
            if (elements.formLoginPageAdmin) elements.formLoginPageAdmin.style.display = 'flex';
            if (elements.formLoginPageStudent) elements.formLoginPageStudent.style.display = 'none';
            if (elements.loginPageFeedback) elements.loginPageFeedback.style.display = 'none';
        });
    }

    // 1-Click Demo Autofill Chips
    if (elements.btnChipDemoStudent) {
        elements.btnChipDemoStudent.addEventListener('click', () => {
            if (elements.loginStudentId) elements.loginStudentId.value = '24AM119';
            if (elements.loginStudentPwd) elements.loginStudentPwd.value = 'Candidate@2026';
            if (elements.loginPageFeedback) elements.loginPageFeedback.style.display = 'none';
        });
    }

    if (elements.btnChipDemoAdmin) {
        elements.btnChipDemoAdmin.addEventListener('click', () => {
            if (elements.loginAdminId) elements.loginAdminId.value = 'admin';
            if (elements.loginAdminPwd) elements.loginAdminPwd.value = 'Admin@2026';
            if (elements.loginPageFeedback) elements.loginPageFeedback.style.display = 'none';
        });
    }

    // Forgot password modal shortcut
    if (elements.btnPageForgotPw) {
        elements.btnPageForgotPw.addEventListener('click', () => {
            if (elements.modalSupabaseAuth) elements.modalSupabaseAuth.style.display = 'flex';
            if (elements.formAuthSignin) elements.formAuthSignin.style.display = 'none';
            if (elements.formAuthReset) {
                elements.formAuthReset.style.display = 'block';
                if (elements.loginStudentId && elements.authResetEmail) {
                    elements.authResetEmail.value = elements.loginStudentId.value;
                }
            }
        });
    }

    // Student Form Submit
    if (elements.formLoginPageStudent) {
        elements.formLoginPageStudent.addEventListener('submit', async (e) => {
            e.preventDefault();
            const id = elements.loginStudentId ? elements.loginStudentId.value.trim() : '';
            const pwd = elements.loginStudentPwd ? elements.loginStudentPwd.value : '';
            await handlePortalLogin(id, pwd, 'STUDENT', elements.btnSubmitPageStudent);
        });
    }

    // Admin Form Submit
    if (elements.formLoginPageAdmin) {
        elements.formLoginPageAdmin.addEventListener('submit', async (e) => {
            e.preventDefault();
            const id = elements.loginAdminId ? elements.loginAdminId.value.trim() : '';
            const pwd = elements.loginAdminPwd ? elements.loginAdminPwd.value : '';
            await handlePortalLogin(id, pwd, 'ADMIN', elements.btnSubmitPageAdmin);
        });
    }
}

async function handlePortalLogin(identifier, password, role, submitBtn) {
    if (!identifier || !password) {
        showLoginPageFeedback("Please enter both your identifier and password.", "error");
        return;
    }

    const defaultBtnText = role === 'ADMIN' ? '🛡️ Sign In as Administrator →' : '🚀 Sign In & Enter Interview Room →';
    setAuthButtonLoading(submitBtn, true, 'Signing in...');
    showLoginPageFeedback("Authenticating credentials...", "info");

    try {
        let backendAuthSuccess = false;
        let resolvedUser = null;
        let effectiveRole = role;

        // 1. Direct Backend Authentication with Spring Boot + Database
        try {
            const res = await fetch(`${API_BASE}/api/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    usernameOrEmail: identifier,
                    password: password,
                    role: role
                })
            });

            if (res.ok) {
                const loginData = await res.json();
                if (loginData.success) {
                    backendAuthSuccess = true;
                    effectiveRole = loginData.role || role;
                    resolvedUser = loginData.user;
                }
            }
        } catch (beErr) {
            console.warn("Backend direct login check bypassed/skipped:", beErr);
        }

        // 2. Supabase Auth Integration (only if backend auth did not already succeed)
        if (state.supabase.client && !backendAuthSuccess) {
            let emailForSupabase = identifier.includes('@') ? identifier : (identifier === 'admin' ? 'admin@interview.ai' : 'alex.morgan@testcampus.edu');
            try {
                let { data, error } = await state.supabase.client.auth.signInWithPassword({
                    email: emailForSupabase,
                    password: password
                });

                if (error && (identifier.toLowerCase().includes('admin') || identifier.toLowerCase().includes('alex') || identifier.toUpperCase() === '24AM119')) {
                    const fallbackRole = (identifier.toLowerCase().includes('admin') || role === 'ADMIN') ? 'admin' : 'student';
                    const fallbackName = fallbackRole === 'admin' ? 'Administrator' : 'Alex Morgan';
                    const signUpRes = await state.supabase.client.auth.signUp({
                        email: emailForSupabase,
                        password: password,
                        options: { data: { full_name: fallbackName, role: fallbackRole, roll_no: '24AM119' } }
                    });
                    if (signUpRes.data && signUpRes.data.user) {
                        data = signUpRes.data;
                        error = null;
                    }
                }

                if (!error && data && data.user) {
                    resolvedUser = {
                        id: data.user.id,
                        email: data.user.email,
                        name: data.user.user_metadata?.full_name || (effectiveRole === 'ADMIN' ? 'Administrator' : 'Candidate'),
                        role: effectiveRole,
                        rollNo: data.user.user_metadata?.roll_no || (effectiveRole === 'STUDENT' ? identifier : null)
                    };
                    backendAuthSuccess = true;
                }
            } catch (sbErr) {
                console.warn("Supabase auth check bypassed:", sbErr);
            }
        }

        // 3. Fallback for Demo Accounts
        if (!backendAuthSuccess && !resolvedUser) {
            if ((identifier.toUpperCase() === '24AM119' || identifier.toLowerCase() === 'alex.morgan@testcampus.edu') && password === 'Candidate@2026') {
                resolvedUser = {
                    id: 'student-demo',
                    name: 'Alex Morgan',
                    email: 'alex.morgan@testcampus.edu',
                    rollNo: '24AM119',
                    studentYear: '2nd Year',
                    department: 'Artificial Intelligence & Machine Learning',
                    role: 'STUDENT'
                };
                effectiveRole = 'STUDENT';
                backendAuthSuccess = true;
            } else if (identifier.toLowerCase() === 'admin' && password === 'Admin@2026') {
                resolvedUser = {
                    id: 'admin-demo',
                    name: 'Chief Administrator',
                    email: 'admin@interview.ai',
                    role: 'ADMIN'
                };
                effectiveRole = 'ADMIN';
                backendAuthSuccess = true;
            }
        }

        if (!backendAuthSuccess || !resolvedUser) {
            throw new Error("Invalid username/roll number or password. Please verify your credentials.");
        }

        // Apply Authenticated User State
        state.authRole = effectiveRole;
        localStorage.setItem('user_role', effectiveRole);
        localStorage.setItem('auth_user', JSON.stringify(resolvedUser));

        const pseudoUser = {
            id: resolvedUser.id || 'auth-user',
            email: resolvedUser.email || (effectiveRole === 'ADMIN' ? 'admin@interview.ai' : `${identifier.toLowerCase()}@testcampus.edu`),
            user_metadata: {
                full_name: resolvedUser.name || (effectiveRole === 'ADMIN' ? 'Administrator' : 'Candidate'),
                role: effectiveRole.toLowerCase(),
                roll_no: resolvedUser.rollNo || (effectiveRole === 'STUDENT' ? identifier.toUpperCase() : null)
            }
        };

        state.supabase.user = pseudoUser;
        updateAuthUI(pseudoUser);

        // Pre-fill student info
        if (effectiveRole === 'STUDENT') {
            if (elements.candidateNameInput) {
                elements.candidateNameInput.value = resolvedUser.name || 'Alex Morgan';
                state.candidateName = elements.candidateNameInput.value;
            }
            if (resolvedUser.email) {
                loadCandidateAssignedInterviews(resolvedUser.email);
            }
        }

        showLoginPageFeedback(`Welcome, ${resolvedUser.name || identifier}! Entering interview room...`, "success");

        setTimeout(() => {
            switchStep('step-setup');
            if (effectiveRole === 'ADMIN') {
                if (elements.modalAdminPortal) elements.modalAdminPortal.style.display = 'flex';
                loadAdminSchedules();
            }
        }, 500);

    } catch (err) {
        showLoginPageFeedback(err.message || "Failed to sign in. Please verify your credentials.", "error");
    } finally {
        setAuthButtonLoading(submitBtn, false, defaultBtnText);
    }
}

function showLoginPageFeedback(msg, type = "info") {
    if (!elements.loginPageFeedback) return;
    elements.loginPageFeedback.className = `modal-feedback-box ${type}`;
    elements.loginPageFeedback.textContent = msg;
    elements.loginPageFeedback.style.display = 'block';
}

function initPortalNavigation() {
    const savedUserStr = localStorage.getItem('auth_user');
    const savedRole = localStorage.getItem('user_role') || 'STUDENT';

    if (savedUserStr) {
        try {
            const savedUser = JSON.parse(savedUserStr);
            if (savedUser && (savedUser.email || savedUser.name || savedUser.rollNo)) {
                state.authRole = savedRole;
                const pseudoUser = {
                    id: savedUser.id || 'auth-user',
                    email: savedUser.email || (savedRole === 'ADMIN' ? 'admin@interview.ai' : 'alex.morgan@testcampus.edu'),
                    user_metadata: {
                        full_name: savedUser.name || (savedRole === 'ADMIN' ? 'Administrator' : 'Alex Morgan'),
                        role: savedRole.toLowerCase(),
                        roll_no: savedUser.rollNo || (savedRole === 'STUDENT' ? '24AM119' : null)
                    }
                };
                state.supabase.user = pseudoUser;
                updateAuthUI(pseudoUser);

                if (savedRole === 'STUDENT') {
                    if (elements.candidateNameInput && savedUser.name) {
                        elements.candidateNameInput.value = savedUser.name;
                        state.candidateName = savedUser.name;
                    }
                    if (savedUser.email) {
                        loadCandidateAssignedInterviews(savedUser.email);
                    }
                }
                switchStep('step-setup');
                return;
            }
        } catch (e) {
            console.warn("Could not parse saved user session:", e);
        }
    }

    // Default Entry: Always start from Login Page
    switchStep('step-login');
}

// ----------------------------------------------------
// ADMIN PORTAL & CANDIDATE INTERVIEW SCHEDULING
// ----------------------------------------------------

async function initAdminAndScheduling() {
    setupAdminEventListeners();
    await loadAdminSchedules();

    // Check if candidate has an active session to resume (network resilience)
    checkNetworkResilienceResumption();
}

function mountAdminInline() {
    const mount = document.getElementById('admin-inline-mount');
    const modal = elements.modalAdminPortal;
    if (!mount || !modal) return;
    const inner = modal.querySelector('.admin-modal');
    if (!inner) return;
    inner._originalParent = modal;
    mount.appendChild(inner);
    inner.classList.add('admin-inline-mode');
    const closeBtn = inner.querySelector('.btn-modal-close');
    if (closeBtn) closeBtn.style.display = 'none';
    modal.style.display = 'none';
}

function unmountAdminInline() {
    const mount = document.getElementById('admin-inline-mount');
    if (!mount) return;
    const inner = mount.querySelector('.admin-modal');
    if (!inner || !inner._originalParent) return;
    inner.classList.remove('admin-inline-mode');
    const closeBtn = inner.querySelector('.btn-modal-close');
    if (closeBtn) closeBtn.style.display = '';
    inner._originalParent.appendChild(inner);
}

function setupAdminEventListeners() {
    // Open/Close Admin Portal
    if (elements.btnBannerOpenAdmin) {
        elements.btnBannerOpenAdmin.addEventListener('click', () => {
            elements.modalAdminPortal.style.display = 'flex';
            loadAdminSchedules();
        });
    }

    if (elements.btnStartPractice) {
        elements.btnStartPractice.addEventListener('click', () => {
            state.candidateSchedule.activeAssignment = null;
            if (elements.activeDrivePillBox) elements.activeDrivePillBox.style.display = 'none';
            if (elements.resumeGateCard) {
                elements.resumeGateCard.style.display = 'block';
                if (elements.resumeGateTitle) elements.resumeGateTitle.textContent = 'Upload Your Resume — Practice Interview';
                if (elements.resumeGateDesc) elements.resumeGateDesc.textContent = 'Upload your resume or CV. The AI will generate practice questions based on your skills. No time limit enforced.';
            }
            const scrollTarget = elements.resumeGateCard || elements.candidateForm;
            scrollTarget.scrollIntoView({ behavior: 'smooth' });
        });
    }

    if (elements.btnCloseAdminPortal) {
        elements.btnCloseAdminPortal.addEventListener('click', () => {
            elements.modalAdminPortal.style.display = 'none';
        });
    }

    // Close on background click
    [elements.modalAdminPortal, elements.modalAdminScorecard, elements.modalChangePassword].forEach(modal => {
        if (modal) {
            modal.addEventListener('click', (e) => {
                if (e.target === modal) modal.style.display = 'none';
            });
        }
    });

    // Admin Navigation Tabs
    const adminTabs = [
        { btn: elements.tabBtnSchedule, panel: elements.panelAdminSchedule },
        { btn: elements.tabBtnAssign, panel: elements.panelAdminAssign },
        { btn: elements.tabBtnMarks, panel: elements.panelAdminMarks }
    ];

    adminTabs.forEach(({ btn, panel }) => {
        if (btn && panel) {
            btn.addEventListener('click', () => {
                adminTabs.forEach(t => {
                    t.btn.classList.remove('active');
                    t.panel.style.display = 'none';
                });
                btn.classList.add('active');
                panel.style.display = 'block';

                if (btn === elements.tabBtnMarks && elements.marksInterviewSelect && elements.marksInterviewSelect.value) {
                    loadAdminAttendanceMarks(elements.marksInterviewSelect.value);
                }
            });
        }
    });

    // Subtabs in Assign Panel (Bulk Assign by Year vs Single Candidate)
    if (elements.subtabAssignYear && elements.subtabAssignSingle) {
        elements.subtabAssignYear.addEventListener('click', () => {
            elements.subtabAssignYear.classList.add('active');
            elements.subtabAssignSingle.classList.remove('active');
            if (elements.formAssignYear) elements.formAssignYear.style.display = 'grid';
            if (elements.formAssignCandidate) elements.formAssignCandidate.style.display = 'none';
        });
        elements.subtabAssignSingle.addEventListener('click', () => {
            elements.subtabAssignSingle.classList.add('active');
            elements.subtabAssignYear.classList.remove('active');
            if (elements.formAssignCandidate) elements.formAssignCandidate.style.display = 'grid';
            if (elements.formAssignYear) elements.formAssignYear.style.display = 'none';
        });
    }

    // Forms
    if (elements.formCreateSchedule) {
        elements.formCreateSchedule.addEventListener('submit', handleAdminScheduleSubmit);
    }

    if (elements.formCreateStudent) {
        elements.formCreateStudent.addEventListener('submit', handleCreateStudentSubmit);
    }

    if (elements.formAssignYear) {
        elements.formAssignYear.addEventListener('submit', handleAssignYearSubmit);
    }

    if (elements.formAssignCandidate) {
        elements.formAssignCandidate.addEventListener('submit', handleAdminAssignSubmit);
    }

    if (elements.btnGenerateStudentPw) {
        elements.btnGenerateStudentPw.addEventListener('click', () => {
            const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%';
            let pw = 'Student@';
            for (let i = 0; i < 6; i++) {
                pw += chars.charAt(Math.floor(Math.random() * chars.length));
            }
            if (elements.studentPassword) {
                elements.studentPassword.value = pw;
            }
        });
    }

    if (elements.btnGeneratePassword) {
        elements.btnGeneratePassword.addEventListener('click', () => {
            const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%';
            let pw = 'Cand@';
            for (let i = 0; i < 6; i++) {
                pw += chars.charAt(Math.floor(Math.random() * chars.length));
            }
            if (elements.assignCandidatePassword) {
                elements.assignCandidatePassword.value = pw;
            }
        });
    }

    if (elements.btnCopyCredentials) {
        elements.btnCopyCredentials.addEventListener('click', () => {
            const drive = elements.credDriveTitle ? elements.credDriveTitle.textContent : '';
            const name = elements.credName ? elements.credName.textContent : '';
            const roll = elements.credRollNo ? elements.credRollNo.textContent : '';
            const email = elements.credEmail ? elements.credEmail.textContent : '';
            const pw = elements.credPassword ? elements.credPassword.textContent : '';
            const rollLine = roll && roll !== '--' ? `Roll No: ${roll}\n` : '';
            const textToCopy = `AI Interview Invitation\nInterview: ${drive}\nCandidate: ${name}\n${rollLine}Login Email: ${email}\nTemporary Password: ${pw}\nPortal: ${window.location.origin}\nNote: Strictly 1 attempt allowed.`;

            navigator.clipboard.writeText(textToCopy).then(() => {
                const btn = elements.btnCopyCredentials;
                const oldText = btn.innerHTML;
                btn.innerHTML = '<span>✅ Copied to Clipboard!</span>';
                setTimeout(() => { btn.innerHTML = oldText; }, 2000);
            }).catch(err => {
                console.error("Clipboard copy failed:", err);
            });
        });
    }

    if (elements.marksInterviewSelect) {
        elements.marksInterviewSelect.addEventListener('change', (e) => {
            if (e.target.value) {
                loadAdminAttendanceMarks(e.target.value);
            }
        });
    }

    if (elements.btnRefreshMarks) {
        elements.btnRefreshMarks.addEventListener('click', () => {
            if (elements.marksInterviewSelect && elements.marksInterviewSelect.value) {
                loadAdminAttendanceMarks(elements.marksInterviewSelect.value);
            }
        });
    }

    if (elements.btnExportExcel) {
        elements.btnExportExcel.addEventListener('click', () => {
            const interviewId = elements.marksInterviewSelect ? elements.marksInterviewSelect.value : null;
            if (!interviewId) {
                alert("Please select an interview drive first.");
                return;
            }
            const link = document.createElement('a');
            link.href = `${API_BASE}/api/admin/schedule/interviews/${interviewId}/export`;
            link.download = '';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        });
    }

    // Clear Active Drive Pill
    if (elements.btnClearActiveDrive) {
        elements.btnClearActiveDrive.addEventListener('click', () => {
            state.candidateSchedule.activeAssignment = null;
            sessionStorage.removeItem('active_assignment_id');
            sessionStorage.removeItem('active_session_token');
            if (elements.activeDrivePillBox) elements.activeDrivePillBox.style.display = 'none';
        });
    }

    // Change Password Modal
    if (elements.btnOpenChangePw) {
        elements.btnOpenChangePw.addEventListener('click', () => {
            elements.modalChangePassword.style.display = 'flex';
            if (elements.changePwFeedback) elements.changePwFeedback.style.display = 'none';
            if (elements.inputNewPassword) elements.inputNewPassword.value = '';
            if (elements.inputConfirmPassword) elements.inputConfirmPassword.value = '';
        });
    }

    if (elements.btnCloseChangePw) {
        elements.btnCloseChangePw.addEventListener('click', () => {
            elements.modalChangePassword.style.display = 'none';
        });
    }

    if (elements.formChangePassword) {
        elements.formChangePassword.addEventListener('submit', handlePasswordChangeSubmit);
    }

    if (elements.btnCloseAdminScorecard) {
        elements.btnCloseAdminScorecard.addEventListener('click', () => {
            elements.modalAdminScorecard.style.display = 'none';
        });
    }
}

async function loadAdminSchedules() {
    try {
        const res = await fetch(`${API_BASE}/api/admin/schedule/interviews`);
        if (!res.ok) throw new Error("Could not load schedules");
        const list = await res.json();
        state.admin.schedules = list;

        // Render Active Schedules Mini List
        if (elements.activeSchedulesList) {
            elements.activeSchedulesList.innerHTML = '';
            if (list.length === 0) {
                elements.activeSchedulesList.innerHTML = `<div class="empty-state-muted">No scheduled interviews created yet. Use the form above to schedule your first interview drive.</div>`;
            } else {
                list.forEach(item => {
                    const card = document.createElement('div');
                    card.className = 'schedule-mini-card';
                    card.innerHTML = `
                        <div class="sched-mini-header">
                            <span class="sched-mini-title">${escapeHtml(item.title)}</span>
                            <span class="sched-mini-role">${escapeHtml(item.targetRole)}</span>
                        </div>
                        <div class="sched-mini-meta">
                            <span>⏱️ ${item.durationMinutes} mins</span>
                            <span>❓ ${item.totalQuestions} questions</span>
                            <span>🎯 Batch: ${escapeHtml(item.targetYear || 'ALL')}</span>
                            <span>👥 ${item.totalAssigned || item.totalAssignedCandidates || 0} assigned</span>
                            <span>✅ ${item.completedCount || item.completedCandidates || 0} completed</span>
                        </div>
                        <div class="sched-mini-actions">
                            <button type="button" class="btn-secondary btn-sm btn-export-interview" data-interview-id="${item.id}">📥 Export Excel</button>
                        </div>
                    `;
                    const exportBtn = card.querySelector('.btn-export-interview');
                    if (exportBtn) {
                        exportBtn.addEventListener('click', () => {
                            const link = document.createElement('a');
                            link.href = `${API_BASE}/api/admin/schedule/interviews/${item.id}/export`;
                            link.download = '';
                            document.body.appendChild(link);
                            link.click();
                            document.body.removeChild(link);
                        });
                    }
                    elements.activeSchedulesList.appendChild(card);
                });
            }
        }

        // Populate Assign & Marks dropdowns
        const assignSelect = elements.assignInterviewSelect;
        const assignYearSelect = elements.assignYearInterviewSelect;
        const marksSelect = elements.marksInterviewSelect;

        if (assignSelect) {
            const currentVal = assignSelect.value;
            assignSelect.innerHTML = `<option value="">-- Choose Scheduled Interview --</option>`;
            list.forEach(item => {
                const opt = document.createElement('option');
                opt.value = item.id;
                opt.textContent = `${item.title} (${item.targetRole} - ${item.durationMinutes}m)`;
                assignSelect.appendChild(opt);
            });
            if (currentVal && list.some(i => i.id === currentVal)) {
                assignSelect.value = currentVal;
            } else if (list.length > 0) {
                assignSelect.value = list[0].id;
            }
        }

        if (assignYearSelect) {
            const currentVal = assignYearSelect.value;
            assignYearSelect.innerHTML = `<option value="">-- Choose Scheduled Interview --</option>`;
            list.forEach(item => {
                const opt = document.createElement('option');
                opt.value = item.id;
                opt.textContent = `${item.title} (Batch: ${item.targetYear || 'ALL'})`;
                assignYearSelect.appendChild(opt);
            });
            if (currentVal && list.some(i => i.id === currentVal)) {
                assignYearSelect.value = currentVal;
            } else if (list.length > 0) {
                assignYearSelect.value = list[0].id;
            }
        }

        if (marksSelect) {
            const currentVal = marksSelect.value;
            marksSelect.innerHTML = `<option value="">-- Choose Scheduled Interview --</option>`;
            list.forEach(item => {
                const opt = document.createElement('option');
                opt.value = item.id;
                opt.textContent = `${item.title} (${item.targetRole})`;
                marksSelect.appendChild(opt);
            });
            if (currentVal && list.some(i => i.id === currentVal)) {
                marksSelect.value = currentVal;
            } else if (list.length > 0) {
                marksSelect.value = list[0].id;
                loadAdminAttendanceMarks(list[0].id);
            }
        }
    } catch (err) {
        console.error("Failed to load admin schedules:", err);
    }
}

async function handleAdminScheduleSubmit(e) {
    e.preventDefault();
    const title = elements.schedTitle.value.trim();
    const role = elements.schedRole.value.trim();
    const duration = parseInt(elements.schedDuration.value, 10);
    const questions = parseInt(elements.schedQuestions.value, 10);
    const desc = elements.schedDescription.value.trim();
    const targetYear = elements.schedTargetYear ? elements.schedTargetYear.value : 'ALL';

    if (!title || !role) return;

    try {
        const btn = document.getElementById('btn-submit-schedule');
        if (btn) btn.disabled = true;
        showAdminFeedback(elements.scheduleFeedback, "Publishing interview drive...", "info");

        const res = await fetch(`${API_BASE}/api/admin/schedule/interviews`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                title: title,
                targetRole: role,
                durationMinutes: duration,
                totalQuestions: questions,
                description: desc,
                targetYear: targetYear
            })
        });

        if (!res.ok) throw new Error("Could not create interview drive");
        const created = await res.json();

        showAdminFeedback(elements.scheduleFeedback, `Interview Drive "${created.title}" successfully published for ${created.targetYear || 'ALL'}!`, "success");
        elements.formCreateSchedule.reset();
        elements.schedDuration.value = 15;
        elements.schedQuestions.value = 7;
        elements.schedRole.value = "Backend Software Engineer";

        await loadAdminSchedules();
    } catch (err) {
        showAdminFeedback(elements.scheduleFeedback, err.message, "error");
    } finally {
        const btn = document.getElementById('btn-submit-schedule');
        if (btn) btn.disabled = false;
    }
}

async function handleCreateStudentSubmit(e) {
    e.preventDefault();
    const name = elements.studentName.value.trim();
    const rollNo = elements.studentRollNo ? elements.studentRollNo.value.trim().toUpperCase() : '';
    const email = elements.studentEmail.value.trim();
    const studentYear = elements.studentYear.value;
    const dept = elements.studentDept.value.trim();
    const password = elements.studentPassword.value;

    if (!name || !email || !password) return;

    try {
        if (elements.btnSubmitCreateStudent) elements.btnSubmitCreateStudent.disabled = true;
        showAdminFeedback(elements.createStudentFeedback, "Saving permanent student account...", "info");

        const res = await fetch(`${API_BASE}/api/admin/schedule/students`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                name,
                email,
                rollNo,
                studentYear,
                department: dept,
                password
            })
        });

        if (!res.ok) throw new Error("Could not save student account");
        const student = await res.json();

        // Also register with Supabase Auth if client is active
        if (state.supabase.client) {
            try {
                await state.supabase.client.auth.signUp({
                    email: email,
                    password: password,
                    options: { data: { full_name: name, role: 'student', roll_no: rollNo, year: studentYear, department: dept } }
                });
            } catch (ignore) {}
        }

        const rollDisplay = student.rollNo || rollNo;
        showAdminFeedback(elements.createStudentFeedback, `✅ Permanent credentials created for ${student.name} ${rollDisplay ? `[Roll: ${rollDisplay}]` : ''} (${student.studentYear})!`, "success");

        if (elements.credentialsCard) {
            elements.credentialsCard.style.display = 'block';
            if (elements.credDriveTitle) elements.credDriveTitle.textContent = `Permanent Student Credentials (${student.studentYear})`;
            if (elements.credName) elements.credName.textContent = student.name;
            if (elements.credRollNo) elements.credRollNo.textContent = student.rollNo || rollNo || '--';
            if (elements.credEmail) elements.credEmail.textContent = student.email;
            if (elements.credPassword) elements.credPassword.textContent = student.rawPassword || password;
            if (elements.credYearDept) elements.credYearDept.textContent = `${student.studentYear} • ${student.department}`;
            elements.credentialsCard.scrollIntoView({ behavior: 'smooth' });
        }
    } catch (err) {
        showAdminFeedback(elements.createStudentFeedback, err.message, "error");
    } finally {
        if (elements.btnSubmitCreateStudent) elements.btnSubmitCreateStudent.disabled = false;
    }
}

async function handleAssignYearSubmit(e) {
    e.preventDefault();
    const interviewId = elements.assignYearInterviewSelect.value;
    const targetYear = elements.assignTargetYearSelect.value;

    if (!interviewId) {
        showAdminFeedback(elements.assignYearFeedback, "Please select an interview drive.", "error");
        return;
    }

    try {
        if (elements.btnSubmitAssignYear) elements.btnSubmitAssignYear.disabled = true;
        showAdminFeedback(elements.assignYearFeedback, `Assigning drive to all ${targetYear} students...`, "info");

        const res = await fetch(`${API_BASE}/api/admin/schedule/assign-year`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ interviewId, targetYear })
        });

        if (!res.ok) throw new Error("Could not assign drive to year");
        const data = await res.json();

        showAdminFeedback(elements.assignYearFeedback, `✅ ${data.message}`, "success");
        await loadAdminSchedules();
        if (elements.marksInterviewSelect && elements.marksInterviewSelect.value === interviewId) {
            loadAdminAttendanceMarks(interviewId);
        }
    } catch (err) {
        showAdminFeedback(elements.assignYearFeedback, err.message, "error");
    } finally {
        if (elements.btnSubmitAssignYear) elements.btnSubmitAssignYear.disabled = false;
    }
}

async function handleAdminAssignSubmit(e) {
    e.preventDefault();
    const interviewId = elements.assignInterviewSelect.value;
    const rollNo = elements.assignCandidateRollNo ? elements.assignCandidateRollNo.value.trim().toUpperCase() : '';

    if (!interviewId || !rollNo) {
        showAdminFeedback(elements.assignFeedback, "Please select an interview and enter a roll number.", "error");
        return;
    }

    try {
        const btn = document.getElementById('btn-submit-assign');
        if (btn) btn.disabled = true;
        showAdminFeedback(elements.assignFeedback, "Looking up student and assigning...", "info");

        const res = await fetch(`${API_BASE}/api/admin/schedule/assign-by-roll`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                interviewId: interviewId,
                rollNo: rollNo
            })
        });

        if (!res.ok) {
            const errData = await res.json().catch(() => ({}));
            throw new Error(errData.error || "Failed to assign student. Make sure the roll number exists.");
        }

        const assignment = await res.json();

        showAdminFeedback(elements.assignFeedback, `Student ${assignment.candidateName || rollNo} successfully assigned!`, "success");

        if (elements.assignCandidateRollNo) elements.assignCandidateRollNo.value = '';

        await loadAdminSchedules();
    } catch (err) {
        showAdminFeedback(elements.assignFeedback, err.message, "error");
    } finally {
        const btn = document.getElementById('btn-submit-assign');
        if (btn) btn.disabled = false;
    }
}

async function loadAdminAttendanceMarks(interviewId) {
    if (!interviewId) return;

    try {
        const res = await fetch(`${API_BASE}/api/admin/schedule/interviews/${interviewId}/attendance`);
        if (!res.ok) throw new Error("Could not load attendance and marks");

        const records = await res.json();

        // Update Stat Counters
        const total = records.length;
        const completed = records.filter(r => r.status === 'COMPLETED').length;
        const malpracticeCount = records.filter(r => r.isMalpractice === true || r.status === 'DISQUALIFIED').length;

        const scored = records.filter(r => r.overallScore != null);
        let avgScore = "--";
        if (scored.length > 0) {
            const sum = scored.reduce((acc, r) => acc + r.overallScore, 0);
            avgScore = (sum / scored.length).toFixed(1) + "/10";
        }

        if (elements.statTotalAssigned) elements.statTotalAssigned.textContent = total;
        if (elements.statTotalCompleted) elements.statTotalCompleted.textContent = completed;
        if (elements.statTotalMalpractice) elements.statTotalMalpractice.textContent = malpracticeCount;
        if (elements.statAvgScore) elements.statAvgScore.textContent = avgScore;

        // Render Table Body
        const tbody = elements.attendanceMarksTbody;
        tbody.innerHTML = '';

        if (records.length === 0) {
            tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-muted">No candidates assigned to this interview drive yet.</td></tr>`;
            return;
        }

        records.forEach(r => {
            const tr = document.createElement('tr');
            const isMalp = r.isMalpractice === true || r.status === 'DISQUALIFIED';

            // CRITICAL REQUIREMENT: Bold RED highlighting for malpractice
            if (isMalp) {
                tr.className = 'row-malpractice';
            }

            // Tech, Comm, Overall Scores
            const techText = r.technicalScore != null ? r.technicalScore.toFixed(1) + '/10' : '--';
            const commText = r.communicationScore != null ? r.communicationScore.toFixed(1) + '/10' : '--';
            const overallText = r.overallScore != null ? `<strong>${r.overallScore.toFixed(1)}/10</strong>` : '--';

            // Verdict Badge
            let verdictBadge = `<span class="badge-subtle">${escapeHtml(r.status || 'PENDING')}</span>`;
            if (isMalp) {
                verdictBadge = `<span class="badge-malpractice-red">🚨 DISQUALIFIED</span>`;
            } else if (r.status === 'COMPLETED') {
                const verdict = r.verdict || 'COMPLETED';
                const vClass = verdict.includes('STRONG HIRE') ? 'badge-strong-hire' : (verdict.includes('HIRE') ? 'badge-hire' : 'badge-subtle');
                verdictBadge = `<span class="${vClass}">${escapeHtml(verdict)}</span>`;
            } else if (r.status === 'IN_PROGRESS') {
                verdictBadge = `<span class="badge-in-progress">In Progress</span>`;
            }

            // Malpractice / Proctor Audit Column (Bold RED indicator)
            let proctorAuditHtml = `<span class="text-muted">No violations</span>`;
            if (isMalp || (r.violationCount && r.violationCount > 0)) {
                const details = r.violationDetails || 'Exceeded policy violation threshold';
                proctorAuditHtml = `<div class="proctor-alert-cell">
                    <span class="violation-count-badge">${r.violationCount || 1} Violations</span>
                    <span class="violation-detail-text" title="${escapeHtml(details)}">${escapeHtml(details)}</span>
                </div>`;
            }

            // Actions Column
            let actionHtml = `<span class="text-muted">--</span>`;
            if (r.sessionId) {
                actionHtml = `<button type="button" class="btn-table-action" onclick="openScorecardDetailModal('${r.sessionId}')">
                    <span>View Scorecard &rarr;</span>
                </button>`;
            }

            const rollNoHtml = r.candidateRollNo
                ? `<span class="badge-subtle-mono"><strong>${escapeHtml(r.candidateRollNo)}</strong></span>`
                : `<span class="text-muted">--</span>`;

            tr.innerHTML = `
                <td><strong>${escapeHtml(r.candidateName || 'Candidate')}</strong></td>
                <td>${rollNoHtml}</td>
                <td><span class="table-email">${escapeHtml(r.candidateEmail || '')}</span></td>
                <td>${techText}</td>
                <td>${commText}</td>
                <td>${overallText}</td>
                <td>${proctorAuditHtml}</td>
                <td>${actionHtml}</td>
            `;

            tbody.appendChild(tr);
        });
    } catch (err) {
        console.error("Failed to load attendance marks:", err);
    }
}

async function openScorecardDetailModal(sessionId) {
    if (!sessionId) return;

    try {
        const res = await fetch(`${API_BASE}/api/admin/schedule/sessions/${sessionId}/scorecard`);
        if (!res.ok) throw new Error("Could not load candidate scorecard");

        const data = await res.json();

        if (elements.adminScorecardCandidate) {
            elements.adminScorecardCandidate.textContent = `${data.candidateName} — Evaluation Scorecard`;
        }
        if (elements.adminScorecardVerdict) {
            elements.adminScorecardVerdict.textContent = `Overall Assessment: ${data.overallVerdict || 'Completed'}`;
        }

        if (elements.adminScorecardContent) {
            let turnsHtml = '';
            if (data.turnSummaries && data.turnSummaries.length > 0) {
                turnsHtml = data.turnSummaries.map(t => `
                    <div class="scorecard-turn-box">
                        <div class="turn-box-header">
                            <span class="turn-num">Q${t.questionNumber}: ${escapeHtml(t.questionText)}</span>
                            <div class="turn-scores-pills">
                                <span class="score-pill">Tech: ${t.technicalScore != null ? t.technicalScore.toFixed(1) : '--'}/10</span>
                                <span class="score-pill">Comm: ${t.communicationScore != null ? t.communicationScore.toFixed(1) : '--'}/10</span>
                            </div>
                        </div>
                        <div class="turn-feedback-body">${escapeHtml(t.feedbackSummary || 'No feedback logged.')}</div>
                    </div>
                `).join('');
            } else {
                turnsHtml = `<p class="text-muted">No individual question turns recorded.</p>`;
            }

            elements.adminScorecardContent.innerHTML = `
                <div class="scorecard-summary-grid">
                    <div class="scorecard-stat">
                        <span class="scorecard-val">${data.overallScore != null ? data.overallScore.toFixed(1) : '--'}/10</span>
                        <span class="scorecard-lbl">Overall Score</span>
                    </div>
                    <div class="scorecard-stat">
                        <span class="scorecard-val">${data.technicalAverage != null ? data.technicalAverage.toFixed(1) : '--'}/10</span>
                        <span class="scorecard-lbl">Technical Avg</span>
                    </div>
                    <div class="scorecard-stat">
                        <span class="scorecard-val">${data.communicationAverage != null ? data.communicationAverage.toFixed(1) : '--'}/10</span>
                        <span class="scorecard-lbl">Communication Avg</span>
                    </div>
                </div>
                <div class="scorecard-detailed-feedback mt-3">
                    <h5>Summary Evaluation</h5>
                    <p>${escapeHtml(data.detailedFeedback || 'Candidate interview completed.')}</p>
                </div>
                <div class="scorecard-turns-section mt-4">
                    <h5>Question Turn Transcripts & Scores</h5>
                    ${turnsHtml}
                </div>
            `;
        }

        if (elements.modalAdminScorecard) {
            elements.modalAdminScorecard.style.display = 'flex';
        }
    } catch (err) {
        alert("Error loading scorecard: " + err.message);
    }
}

// ----------------------------------------------------
// CANDIDATE ASSIGNED INTERVIEWS & NETWORK RESILIENCE
// ----------------------------------------------------

async function loadCandidateAssignedInterviews(email) {
    if (!email) {
        if (elements.candidateAssignedSection) elements.candidateAssignedSection.style.display = 'none';
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/api/candidate/schedule/my-interviews?email=${encodeURIComponent(email)}`);
        if (!res.ok) throw new Error("Could not load your assigned interviews");

        const list = await res.json();
        state.candidateSchedule.assignedInterviews = list;

        if (elements.assignedInterviewsGrid) {
            elements.assignedInterviewsGrid.innerHTML = '';
            if (elements.pastInterviewsGrid) elements.pastInterviewsGrid.innerHTML = '';

            if (list.length === 0) {
                elements.candidateAssignedSection.style.display = 'none';
                return;
            }

            elements.candidateAssignedSection.style.display = 'block';

            const upcoming = [];
            const past = [];
            list.forEach(a => {
                const s = a.attemptStatus || a.status || 'NOT_STARTED';
                if (s === 'COMPLETED' || s === 'DISQUALIFIED') {
                    past.push(a);
                } else {
                    upcoming.push(a);
                }
            });

            if (elements.assignedInterviewsCount) {
                elements.assignedInterviewsCount.textContent = `${upcoming.length} Upcoming · ${past.length} Past`;
            }

            if (elements.upcomingLabel) {
                elements.upcomingLabel.style.display = upcoming.length > 0 ? '' : 'none';
            }
            if (elements.pastSectionHeader) {
                elements.pastSectionHeader.style.display = past.length > 0 ? '' : 'none';
            }

            const renderCard = (assignment, container) => {
                const card = document.createElement('div');
                card.className = 'assigned-interview-card';

                const status = assignment.attemptStatus || assignment.status || 'NOT_STARTED';
                const isCompleted = status === 'COMPLETED';
                const isDisqualified = status === 'DISQUALIFIED';
                const isInProgress = status === 'IN_PROGRESS';

                let badgeHtml = `<span class="badge-single-attempt">Strictly 1 Attempt Allowed</span>`;
                let actionBtnText = "Start Scheduled Interview";
                let actionDisabled = false;
                let btnClass = "btn-primary";

                let completedMarksHtml = '';
                if (isCompleted) {
                    const score = assignment.overallScore != null ? assignment.overallScore.toFixed(1) : '--';
                    const tech = assignment.technicalScore != null ? assignment.technicalScore.toFixed(1) : '--';
                    const comm = assignment.communicationScore != null ? assignment.communicationScore.toFixed(1) : '--';
                    badgeHtml = `<span class="badge-attempt-used">✅ Attempt Completed</span>`;
                    completedMarksHtml = `
                        <div class="completed-score-summary mt-2">
                            <div class="badge-score-pill">🏆 Marks: <strong>${score}/10</strong> (Tech: ${tech} • Comm: ${comm})</div>
                            ${assignment.sessionId ? `<button type="button" class="btn btn-secondary btn-scorecard-view btn-block mt-2" data-session-view="${assignment.sessionId}">📊 View My Scorecard & Marks</button>` : ''}
                        </div>
                    `;
                    actionBtnText = "Interview Completed";
                    actionDisabled = true;
                    btnClass = "btn-secondary";
                } else if (isDisqualified) {
                    badgeHtml = `<span class="badge-malpractice-red">🚨 Disqualified (Malpractice)</span>`;
                    actionBtnText = "Disqualified — Retakes Barred";
                    actionDisabled = true;
                    btnClass = "btn-danger-disabled";
                } else if (isInProgress) {
                    badgeHtml = `<span class="badge-in-progress">⚡ In Progress / Network Resumption</span>`;
                    actionBtnText = "Resume Interview Session";
                    btnClass = "btn-primary btn-pulse";
                }

                let mustChangePwWarning = '';
                if (assignment.mustChangePassword) {
                    mustChangePwWarning = `<div class="temp-pw-warning">🔑 Temporary password in use. You can update your password using the "Password" button above.</div>`;
                }

                const batchTag = assignment.targetYear && assignment.targetYear !== 'ALL' ? `<span class="badge-single-attempt" style="margin-right: 0.35rem;">🎯 ${escapeHtml(assignment.targetYear)}</span>` : '';

                card.innerHTML = `
                    <div class="assigned-card-top">
                        <h4 class="assigned-drive-title">${escapeHtml(assignment.interviewTitle)}</h4>
                        <div style="display: flex; gap: 0.35rem; align-items: center;">
                            ${batchTag}
                            <span class="assigned-role-tag">${escapeHtml(assignment.targetRole)}</span>
                        </div>
                    </div>
                    <p class="assigned-drive-desc">${escapeHtml(assignment.description || 'Comprehensive technical interview screening.')}</p>
                    <div class="assigned-meta-row">
                        <span>⏱️ ${assignment.durationMinutes} Minutes</span>
                        <span>❓ ${assignment.totalQuestions} Questions</span>
                        ${badgeHtml}
                    </div>
                    ${completedMarksHtml}
                    ${mustChangePwWarning}
                    ${!isCompleted ? `
                    <div class="assigned-action-row mt-3">
                        <button type="button" class="btn ${btnClass} btn-block" ${actionDisabled ? 'disabled' : ''} data-assignment-id="${assignment.assignmentId}">
                            <span>${actionBtnText}</span>
                        </button>
                    </div>` : ''}
                `;

                if (isCompleted) {
                    const viewBtn = card.querySelector('[data-session-view]');
                    if (viewBtn) {
                        viewBtn.addEventListener('click', () => {
                            const sid = viewBtn.getAttribute('data-session-view');
                            viewCandidateScorecard(sid);
                        });
                    }
                } else if (!actionDisabled) {
                    const btn = card.querySelector('button');
                    if (btn) {
                        btn.addEventListener('click', () => selectAssignedInterview(assignment));
                    }
                }

                container.appendChild(card);
            };

            upcoming.forEach(a => renderCard(a, elements.assignedInterviewsGrid));
            if (elements.pastInterviewsGrid) {
                past.forEach(a => renderCard(a, elements.pastInterviewsGrid));
            }
        }
    } catch (err) {
        console.error("Failed to load assigned interviews:", err);
    }
}

async function selectAssignedInterview(assignment) {
    let email = state.supabase.user ? state.supabase.user.email : null;
    if (!email) {
        try { email = JSON.parse(localStorage.getItem('auth_user') || '{}').email; } catch (e) {}
    }
    if (!email) {
        alert("Could not determine your email. Please log in again.");
        return;
    }
    const sessionToken = state.candidateSchedule.sessionToken;

    // Pre-flight check: Verify single-attempt eligibility and network resumption
    try {
        const verifyRes = await fetch(`${API_BASE}/api/candidate/schedule/verify-attempt`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                interviewId: assignment.interviewId,
                email: email,
                sessionToken: sessionToken
            })
        });

        if (!verifyRes.ok) {
            throw new Error("Eligibility check failed");
        }

        const eligibility = await verifyRes.json();
        if (!eligibility.allowed) {
            alert(`Interview Access Denied: ${eligibility.message}`);
            if (state.supabase.user) {
                loadCandidateAssignedInterviews(state.supabase.user.email);
            }
            return;
        }

        // Lock in assignment in state
        state.candidateSchedule.activeAssignment = assignment;
        state.totalQuestions = assignment.totalQuestions || 7;
        state.globalExamDuration = (assignment.durationMinutes || 15) * 60;
        state.globalExamTimeRemaining = state.globalExamDuration;

        // Auto-fill candidate name if present
        if (assignment.candidateName && elements.candidateNameInput) {
            elements.candidateNameInput.value = assignment.candidateName;
            state.candidateName = assignment.candidateName;
        }

        // Update selected interview banner
        if (elements.activeDrivePillBox) {
            elements.activeDrivePillBox.style.display = 'flex';
            if (elements.activeDriveTitle) elements.activeDriveTitle.textContent = assignment.interviewTitle;
            if (elements.activeDriveLimits) elements.activeDriveLimits.textContent = `${assignment.durationMinutes} Min • ${assignment.totalQuestions} Qs`;
        }

        // Show the resume gate card with interview-specific context
        if (elements.resumeGateCard) {
            elements.resumeGateCard.style.display = 'block';
            if (elements.resumeGateTitle) {
                elements.resumeGateTitle.textContent = `Upload Your Resume — ${assignment.interviewTitle}`;
            }
            if (elements.resumeGateDesc) {
                elements.resumeGateDesc.textContent = `Upload your resume or CV before starting. The AI interviewer will tailor ${assignment.totalQuestions} questions to your skills. Duration: ${assignment.durationMinutes} minutes.`;
            }
        }

        // Smooth scroll to the resume upload form
        const scrollTarget = elements.resumeGateCard || elements.candidateForm;
        scrollTarget.scrollIntoView({ behavior: 'smooth' });
    } catch (err) {
        console.error("Error during attempt verification:", err);
        alert("Could not verify attempt eligibility: " + err.message);
    }
}

async function checkNetworkResilienceResumption() {
    const activeToken = sessionStorage.getItem('active_session_token');
    const activeAssignmentId = sessionStorage.getItem('active_assignment_id');

    if (!activeToken || !activeAssignmentId) return;

    console.log("Checking potential network resilience auto-resume with token:", activeToken);
    try {
        const email = state.supabase.user ? state.supabase.user.email : null;
        if (!email) return;

        // Look up assignment
        const res = await fetch(`${API_BASE}/api/candidate/schedule/my-interviews?email=${encodeURIComponent(email)}`);
        if (!res.ok) return;
        const list = await res.json();
        const matched = list.find(a => a.assignmentId === activeAssignmentId && a.status === 'IN_PROGRESS');

        if (matched) {
            console.log("Found active in-progress assignment for resumption:", matched.interviewTitle);
            state.candidateSchedule.activeAssignment = matched;
            state.candidateSchedule.sessionToken = activeToken;
            if (elements.activeDrivePillBox) {
                elements.activeDrivePillBox.style.display = 'flex';
                if (elements.activeDriveTitle) elements.activeDriveTitle.textContent = matched.interviewTitle + " (Resuming Session)";
                if (elements.activeDriveLimits) elements.activeDriveLimits.textContent = `${matched.durationMinutes} Min • ${matched.totalQuestions} Qs`;
            }
        }
    } catch (e) {
        console.warn("Auto-resume check skipped:", e);
    }
}

async function handlePasswordChangeSubmit(e) {
    e.preventDefault();
    const newPw = elements.inputNewPassword.value;
    const confirmPw = elements.inputConfirmPassword.value;

    if (!newPw || newPw.length < 6) {
        showAdminFeedback(elements.changePwFeedback, "Password must be at least 6 characters.", "error");
        return;
    }

    if (newPw !== confirmPw) {
        showAdminFeedback(elements.changePwFeedback, "Passwords do not match.", "error");
        return;
    }

    if (!state.supabase.client) {
        showAdminFeedback(elements.changePwFeedback, "Supabase client not active.", "error");
        return;
    }

    try {
        showAdminFeedback(elements.changePwFeedback, "Updating password...", "info");

        const { error } = await state.supabase.client.auth.updateUser({ password: newPw });
        if (error) throw error;

        // Notify backend that temporary password was changed
        if (state.supabase.user && state.supabase.user.email) {
            try {
                await fetch(`${API_BASE}/api/candidate/schedule/password-status`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email: state.supabase.user.email })
                });
            } catch (ignore) {}
        }

        showAdminFeedback(elements.changePwFeedback, "Password updated successfully!", "success");
        setTimeout(() => {
            elements.modalChangePassword.style.display = 'none';
            if (state.supabase.user) {
                loadCandidateAssignedInterviews(state.supabase.user.email);
            }
        }, 1200);
    } catch (err) {
        showAdminFeedback(elements.changePwFeedback, err.message, "error");
    }
}

function showAdminFeedback(container, msg, type = 'info') {
    if (!container) return;
    container.className = `modal-feedback-box ${type}`;
    container.textContent = msg;
    container.style.display = 'block';
}

