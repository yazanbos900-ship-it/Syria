package com.example.features.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.di.ServiceLocator
import com.example.domain.model.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class JobsMarketplaceViewModel : ViewModel() {
    private val jobRepository = ServiceLocator.jobRepository
    
    private val _jobs = MutableStateFlow<List<Job>>(emptyList())
    val jobs: StateFlow<List<Job>> = _jobs.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedLocation = MutableStateFlow<String?>(null)
    val selectedLocation: StateFlow<String?> = _selectedLocation.asStateFlow()

    private val _selectedEmploymentType = MutableStateFlow<String?>(null)
    val selectedEmploymentType: StateFlow<String?> = _selectedEmploymentType.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedExperienceLevel = MutableStateFlow<String?>(null)
    val selectedExperienceLevel: StateFlow<String?> = _selectedExperienceLevel.asStateFlow()

    val availableLocations = listOf("دمشق", "حلب", "حمص", "اللاذقية", "حماة", "طرطوس", "الرقة", "دير الزور", "السويداء", "الحسكة", "درعا", "إدلب", "القنيطرة")
    val availableEmploymentTypes = listOf("دوام كامل", "دوام جزئي", "عن بعد", "عقد", "تطوع", "تدريب", "مستقل")
    val availableCategories = listOf("إدارة أعمال", "تقنية المعلومات", "إدخال بيانات", "لوجستيات", "مبيعات", "تسويق", "هندسة", "طب وصيدلة", "موارد بشرية", "محاسبة", "خدمة عملاء", "تصميم", "أخرى")
    val availableExperienceLevels = listOf("حديث التخرج", "مبتدئ (جونيور)", "متوسط", "خبير (سينيور)", "مدير")

    private var searchJobTracker: kotlinx.coroutines.Job? = null

    init {
        performSearch()
        seedMockJobsIfNeeded()
    }

    private fun seedMockJobsIfNeeded() {
        viewModelScope.launch {
            try {
                val currentJobs = jobRepository.getActiveJobs().first()
                if (currentJobs.isEmpty()) {
                    val defaultJobs = listOf(
                        Job(
                            title = "مطور تطبيقات أندرويد",
                            description = "نبحث عن مطور أندرويد ذو خبرة للانضمام إلى فريقنا وبناء وتطوير تطبيقات مبتكرة.",
                            requirements = "- خبرة لا تقل عن 3 سنوات في تطوير أندرويد\n- إتقان Kotlin و Jetpack Compose\n- فهم قوي لـ MVVM و Clean Architecture",
                            responsibilities = "- تطوير ميزات جديدة\n- تحسين أداء التطبيق\n- مراجعة الكود",
                            location = "دمشق",
                            employmentType = "دوام كامل",
                            category = "تقنية المعلومات",
                            experienceLevel = "خبير (سينيور)",
                            salary = "15,000 - 20,000 ل.س",
                            storeName = "شركة التقنية المتقدمة",
                            isStoreVerified = true
                        ),
                        Job(
                            title = "مصمم واجهات المستخدم (UI/UX)",
                            description = "مطلوب مصمم واجهات مبدع يمتلك مهارات تحليل وتصميم واجهات مستخدم رائعة لتطبيقات الهاتف ومواقع الويب.",
                            requirements = "- خبرة جيدة في استخدام Figma\n- محفظة أعمال قوية\n- فهم عميق لتجربة المستخدم",
                            responsibilities = "- تصميم شاشات جديدة\n- تحسين تجربة المستخدم الحالية\n- العمل مع فريق التطوير",
                            location = "حلب",
                            employmentType = "مستقل",
                            category = "تصميم",
                            experienceLevel = "متوسط",
                            salary = "قابل للتفاوض",
                            storeName = "وكالة الإبداع الرقمي",
                            isStoreVerified = false
                        ),
                        Job(
                            title = "مدير تسويق رقمي",
                            description = "نبحث عن خبير تسويق رقمي لقيادة حملاتنا الإعلانية وزيادة الوعي بعلامتنا التجارية.",
                            requirements = "- خبرة 5 سنوات في التسويق\n- إجادة Google Ads و Facebook Ads\n- مهارات تحليل البيانات",
                            responsibilities = "- وضع خطط تسويقية\n- إدارة ميزانيات الإعلانات\n- تحليل أداء الحملات",
                            location = "اللاذقية",
                            employmentType = "دوام كامل",
                            category = "تسويق",
                            experienceLevel = "مدير",
                            salary = "ثابت بالاضافة لعمولة",
                            storeName = "سوق شامل",
                            isStoreVerified = true
                        )
                    )
                    defaultJobs.forEach { job ->
                        jobRepository.createJob(job)
                    }
                }
            } catch (e: Exception) {
                // Ignore seed error
            }
        }
    }

    private fun loadJobs() {
        performSearch()
    }
    
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        performSearch()
    }

    fun setLocationFilter(location: String?) {
        _selectedLocation.value = location
        performSearch()
    }

    fun setEmploymentTypeFilter(employmentType: String?) {
        _selectedEmploymentType.value = employmentType
        performSearch()
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategory.value = category
        performSearch()
    }

    fun setExperienceLevelFilter(experienceLevel: String?) {
        _selectedExperienceLevel.value = experienceLevel
        performSearch()
    }

    fun clearFilters() {
        _selectedLocation.value = null
        _selectedEmploymentType.value = null
        _selectedCategory.value = null
        _selectedExperienceLevel.value = null
        _searchQuery.value = ""
        performSearch()
    }

    private fun performSearch() {
        searchJobTracker?.cancel()
        searchJobTracker = viewModelScope.launch {
            _isLoading.value = true
            jobRepository.searchJobs(
                query = _searchQuery.value,
                category = _selectedCategory.value,
                location = _selectedLocation.value,
                employmentType = _selectedEmploymentType.value,
                experienceLevel = _selectedExperienceLevel.value
            )
                .catch { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collect { jobList ->
                    _jobs.value = jobList
                    _isLoading.value = false
                }
        }
    }
}
