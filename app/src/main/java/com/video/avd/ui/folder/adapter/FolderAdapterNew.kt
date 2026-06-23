package com.video.avd.ui.folder.adapter


/*class FolderAdapterNew(var list: List<VideoFolder>, var context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var onClickFolder: OnClickListner
    private var filteredVideos: MutableList<VideoFolder> = list.toMutableList()

    private val VIEW_TYPE_VIDEO = 0
    val VIEW_TYPE_AD = 1
    val VIEW_TYPE_AD_LARGE = 2
    var isNativeAdLoaded = false
    var isLargeAdLoaded = false

    private val adPositions = setOf(4, 21, 36)
    private val largeAdPositions = setOf(11, 29)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.d("FolderAdapter", "onCreateViewHolder called with viewType: $viewType")
        val view = LayoutInflater.from(parent.context)
        val holder = FolderAdapterViewHolders()
        return try {
            when (viewType) {
                VIEW_TYPE_VIDEO -> when (VIEW_TYPE.value) {
                    0 -> {
                        Log.d("FolderAdapter", "Inflating video folder layout (List view)")
                        val binding = ItemFolderBinding.inflate(view, parent, false)
                        holder.FolderViewHolder(binding)
                    }
                    1 -> {
                        Log.d("FolderAdapter", "Inflating video folder layout (Grid view)")
                        val binding = ItemFolderGridBinding.inflate(view, parent, false)
                        holder.FolderViewHolderGrid(binding)
                    }
                    else -> throw IllegalArgumentException("Unsupported VIEW_TYPE.value")
                }
                VIEW_TYPE_AD -> {
                    if(recyclerNative){
                        val adBindingapplovin = NativeAdApplovinsmallBinding.inflate(view, parent, false)
                        Log.d("FolderAdapter", "Inflating native ad layout")
                        val adBinding = CustomTemplateListBinding.inflate(view, parent, false)
                        NativeAdViewHolderList(adBinding,adBindingapplovin)
                    }
                    else{
                        when (VIEW_TYPE.value) {
                            0 -> {
                                Log.d("FolderAdapter", "Inflating video folder layout (List view)")
                                val binding = ItemFolderBinding.inflate(view, parent, false)
                                holder.FolderViewHolder(binding)
                            }
                            1 -> {
                                Log.d("FolderAdapter", "Inflating video folder layout (Grid view)")
                                val binding = ItemFolderGridBinding.inflate(view, parent, false)
                                holder.FolderViewHolderGrid(binding)
                            }
                            else -> throw IllegalArgumentException("Unsupported VIEW_TYPE.value")
                        }
                    }

                }
                VIEW_TYPE_AD_LARGE -> {
                    if(recyclerNative){
                        val adBindingapplovin = NativeAdApplovinnewBinding.inflate(view, parent, false)
                        Log.d("FolderAdapter", "Inflating large native ad layout")
                        val adBinding = CustomTemplateLargeListBinding.inflate(view, parent, false)
                        NativeAdViewHolderLargeList(adBinding,adBindingapplovin)
                    }
                    else{
                        when (VIEW_TYPE.value) {
                            0 -> {
                                Log.d("FolderAdapter", "Inflating video folder layout (List view)")
                                val binding = ItemFolderBinding.inflate(view, parent, false)
                                holder.FolderViewHolder(binding)
                            }
                            1 -> {
                                Log.d("FolderAdapter", "Inflating video folder layout (Grid view)")
                                val binding = ItemFolderGridBinding.inflate(view, parent, false)
                                holder.FolderViewHolderGrid(binding)
                            }
                            else -> throw IllegalArgumentException("Unsupported VIEW_TYPE.value")
                        }
                    }
                }
                else -> throw IllegalArgumentException("Unsupported viewType")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val binding = ItemFolderBinding.inflate(view, parent, false)
            holder.FolderViewHolder(binding)
        }
    }

    override fun getItemCount(): Int {
        val regularAdOffset = if (isNativeAdLoaded) adPositions.count { it < filteredVideos.size } else 0
        val largeAdOffset = if (isLargeAdLoaded) largeAdPositions.count { it < (filteredVideos.size + regularAdOffset) } else 0
        val totalCount = filteredVideos.size + regularAdOffset + largeAdOffset
        Log.d("FolderAdapter", "Calculated item count: $totalCount, filteredVideos.size=${filteredVideos.size}")
        return totalCount
    }

    override fun getItemViewType(position: Int): Int {
        var adOffset = 0
        var largeAdOffset = 0

        if (isNativeAdLoaded) {
            adPositions.forEach { if (position >= it + adOffset) adOffset++ }
        }
        if (isLargeAdLoaded) {
            largeAdPositions.forEach { if (position >= it + largeAdOffset) largeAdOffset++ }
        }

        Log.d("FolderAdapter", "getItemViewType: position=$position, adOffset=$adOffset, largeAdOffset=$largeAdOffset")
        return when {
            AdsManager.recyclerNative && isLargeAdLoaded && largeAdPositions.contains(position - adOffset - largeAdOffset) -> VIEW_TYPE_AD_LARGE
            AdsManager.recyclerNative && isNativeAdLoaded && adPositions.contains(position - adOffset) -> VIEW_TYPE_AD
            else -> VIEW_TYPE_VIDEO
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        Log.d("FolderAdapter", "onBindViewHolder called for position: $position")

        var adOffset = 0
        var largeAdOffset = 0

        if (isNativeAdLoaded) {
            adPositions.forEach { if (position > it + adOffset) adOffset++ }
        }
        if (isLargeAdLoaded) {
            largeAdPositions.forEach { if (position > it + largeAdOffset + adOffset) largeAdOffset++ }
        }

        try {
            when (getItemViewType(position)) {
                VIEW_TYPE_VIDEO -> {
                    val videoIndex = position - adOffset - largeAdOffset
                    if (videoIndex in filteredVideos.indices) {
                        val item = filteredVideos[videoIndex]
                        Log.d("FolderAdapter", "Binding video folder: ${item.name} at position $position, videoIndex=$videoIndex")
                        when (VIEW_TYPE.value) {
                            0 -> {
                                (holder as FolderAdapterViewHolders.FolderViewHolder).bind(item)
                                holder.binding.root.setOnClickListener {
                                    onClickFolder.onClickListner(item.id.toString(), item.name)
                                }
                            }
                            1 -> {
                                (holder as FolderAdapterViewHolders.FolderViewHolderGrid).bind(item)
                                holder.binding.root.setOnClickListener {
                                    onClickFolder.onClickListner(item.id.toString(), item.name)
                                }
                            }
                        }
                    } else {
                        Log.e("FolderAdapter", "Invalid videoIndex: $videoIndex for position $position, filteredVideos.size=${filteredVideos.size}")
                    }
                }
                VIEW_TYPE_AD -> Log.d("FolderAdapter", "Binding native ad at position $position")
                VIEW_TYPE_AD_LARGE -> Log.d("FolderAdapter", "Binding large native ad at position $position")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setData(list: List<VideoFolder>) {
        this.list = list
        filteredVideos = list.toMutableList()
        Log.d("FolderAdapter", "setData called with ${filteredVideos.size} items")
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        Log.d("FolderAdapter", "Filter called with query: $query")
        filteredVideos.clear()
        if (query.isEmpty()) {
            filteredVideos.addAll(list)
        } else {
            for (item in list) {
                if (item.name.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault()))) {
                    filteredVideos.add(item)
                }
            }
        }
        Log.d("FolderAdapter", "Filtered list size: ${filteredVideos.size}")
        notifyDataSetChanged()
    }

    fun setOnClickListner(onClickListner: OnClickListner) {
        this.onClickFolder = onClickListner
        Log.d("FolderAdapter", "Click listener set")
    }

    inner class NativeAdViewHolderList(var binding: CustomTemplateListBinding,
                                       var bindingApplovin: NativeAdApplovinsmallBinding
    ) : RecyclerView.ViewHolder( if (AdsManager.adSdkChoice == "admob") binding.root else bindingApplovin.root) {
        fun bind() {
            loadNativeListTemplate(binding,bindingApplovin)
        }
    }

    inner class NativeAdViewHolderLargeList(var binding: CustomTemplateLargeListBinding,
                                            var bindingAppLovin: NativeAdApplovinnewBinding
    ) : RecyclerView.ViewHolder( if (AdsManager.adSdkChoice == "admob") binding.root else bindingAppLovin.root) {
        fun bind() {
            loadNativeListLargeTemplate(binding,bindingAppLovin)
        }
    }

    fun loadNativeListLargeTemplate(adView: CustomTemplateLargeListBinding,
                                    bindingAppLovin: NativeAdApplovinnewBinding
    ) {
        if(recyclerNative){
            when(AdsManager.adSdkChoice){
                "admob"->{
                    try {
                        // The headline and mediaContent are guaranteed to be in every NativeAd.
                        adView.adHeadline.text = AdsManager.nativeAdNow?.headline
                        adView.adBody.text = AdsManager.nativeAdNow?.body
                        adView.adMedia.mediaContent = AdsManager.nativeAdNow?.mediaContent
                        val adappicon = adView.adAppIcon
                        val adAppIconDrawable = AdsManager.nativeAdNow?.icon?.drawable
                        adappicon.setImageDrawable(adAppIconDrawable)
                        val callToActionView = adView.adCallToAction
                        callToActionView.text = AdsManager.nativeAdNow?.callToAction
                        adView.nativeadview.headlineView = adView.adHeadline
                        adView.nativeadview.mediaView = adView.adMedia
                        adView.nativeadview.callToActionView = callToActionView
                        adView.nativeadview.iconView=adappicon
                        AdsManager.nativeAdNow?.let { adView.nativeadview.setNativeAd(it) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                "applovin"->{
                    try {
                        val nativeAdViewApplovin = AppLovinAdUtils.nativeAdViewApplovin
                        if (nativeAdViewApplovin != null) {
                            val parent = nativeAdViewApplovin?.parent as? ViewGroup
                            parent?.removeView(nativeAdViewApplovin)
                            bindingAppLovin.root.removeAllViews()
                            bindingAppLovin.root.addView(nativeAdViewApplovin)
                            Log.d("AppLovinNative", "Adding View to recycler")
                        }
                    } catch (e: Exception) {
                        Log.e("AppLovinNative", "Adding View Exception ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        }

    }

    fun loadNativeListTemplate(adView: CustomTemplateListBinding,
                               bindingApplovin: NativeAdApplovinsmallBinding
    ) {
        if(recyclerNative){
            when(AdsManager.adSdkChoice){
                "admob"->{
                    try {
                        val headlineView = adView.primary
                        headlineView.text = AdsManager.nativeAd?.headline
                        adView.adBody.text = AdsManager.nativeAd?.body
                        val imageView = adView.AdImage
                        imageView.mediaContent= AdsManager.nativeAd?.mediaContent
                        val callToActionView = adView.cta
                        callToActionView.text = AdsManager.nativeAd?.callToAction
                        adView.adViewLayout.headlineView = headlineView
                        adView.adViewLayout.mediaView = imageView
                        adView.adViewLayout.callToActionView = callToActionView
                        AdsManager.nativeAd?.let { adView.adViewLayout.setNativeAd(it) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                "applovin"->{
                    try {
                        val nativeAdViewApplovin = AppLovinAdUtils.nativeAdViewApplovinSmall
                        if (nativeAdViewApplovin != null) {
                            Log.d("AppLovinNative", "Adding View Recycler")
                            val parent = nativeAdViewApplovin?.parent as? ViewGroup
                            parent?.removeView(nativeAdViewApplovin)
                            bindingApplovin.root.removeAllViews()
                            bindingApplovin.root.addView(nativeAdViewApplovin)
                        }
                    } catch (e: Exception) {
                        Log.e("AppLovinNative", "Adding View Exception ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    fun loadNativeAd(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (AdsManager.nativeAd == null){
                    val adLoader = AdLoader.Builder(context,context.resources.getString(R.string.Native_static))
                        .forNativeAd { nativeAd ->
                            AdsManager.nativeAd = nativeAd
                            isNativeAdLoaded = true
                            notifyDataSetChanged()
                        }
                        .withAdListener(object : AdListener() {
                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                isNativeAdLoaded = false
                            }
                        })
                        .build()
                    adLoader.loadAd(AdRequest.Builder().build())
                }else{
                    isNativeAdLoaded = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    fun loadNativeAdLArge(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
            try {
                if (AdsManager.nativeAdNow == null){
                    val adLoader = AdLoader.Builder(context,context.resources.getString(R.string.Native_ID))
                        .forNativeAd { nativeAd ->
                            AdsManager.nativeAdNow = nativeAd
                            isLargeAdLoaded = true
                            notifyDataSetChanged()
                        }
                        .withAdListener(object : AdListener() {
                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                isLargeAdLoaded = false
                            }
                        })
                        .build()
                    adLoader.loadAd(AdRequest.Builder().build())
                }else{
                    isLargeAdLoaded = true
                }
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
    }

    fun loadNativeAdAppLovin(activity: Activity) {
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
            if (AppLovinAdUtils.nativeAdViewApplovin != null) {
                isLargeAdLoaded = true
                // Ad is already loaded, reuse the existing one
                Log.d("AppLovinNative", "Reusing already loaded native ad")
                return@launch
            }
            val adUnitId = activity.resources.getString(R.string.Native_ID_AppLovin)
            Log.d("AppLovinNative", "Initializing native ad loader")
            AppLovinAdUtils.nativeAdLoader = MaxNativeAdLoader(adUnitId, activity)
            val nativeAdView =
                AppLovinAdUtils.createNativeAdView(activity) // Call the function here
            AppLovinAdUtils.nativeAdLoader?.setNativeAdListener(object : MaxNativeAdListener() {
                override fun onNativeAdLoaded(nativeAdView: MaxNativeAdView?, ad: MaxAd) {
                    Log.d("AppLovinNative", "Native Ad Loaded Successfully")
                    AppLovinAdUtils.nativeAdViewApplovin = nativeAdView
                    isLargeAdLoaded = true
                    AppLovinAdUtils.nativeAd = ad
                    notifyDataSetChanged()
                }

                override fun onNativeAdLoadFailed(adUnitId: String, error: MaxError) {
                    Log.e("AppLovinNative", "Native Ad failed to load: ${error.message}")
                    AppLovinAdUtils.nativeAdViewApplovin = null
                    isLargeAdLoaded = false
                }

                override fun onNativeAdClicked(ad: MaxAd) {
                    Log.d("AppLovinNative", "Native Ad clicked")
                }
            })
            // Load the native ad with the custom native ad view
            AppLovinAdUtils.nativeAdLoader?.loadAd(nativeAdView)
        }

    }

    fun loadNativeAdSmall(activity: Activity) {
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
            if (AppLovinAdUtils.nativeAdViewApplovinSmall != null) {
                isNativeAdLoaded = true
                Log.d("AppLovinNative", "Reusing already loaded native ad")
                return@launch
            }
            // Replace with your AppLovin Native Ad Unit ID
            val adUnitId = activity.resources.getString(R.string.Native_ID_AppLovin)
            Log.d("AppLovinNative", "Initializing native ad loader")
            AppLovinAdUtils.nativeAdLoadersmall = MaxNativeAdLoader(adUnitId, activity)

            AppLovinAdUtils.nativeAdLoadersmall?.setNativeAdListener(object : MaxNativeAdListener() {
                override fun onNativeAdLoaded(nativeAdView: MaxNativeAdView?, ad: MaxAd) {
                    Log.d("AppLovinNative", "Native Ad Loaded Successfully")
                    isNativeAdLoaded = true
                    // Remove previous native ad, if any
                    AppLovinAdUtils.nativeAdViewApplovinSmall = nativeAdView
                    AppLovinAdUtils.nativeAdsmall?.let { AppLovinAdUtils.nativeAdLoadersmall?.destroy(it) }
                    AppLovinAdUtils.nativeAdsmall = ad
                    notifyDataSetChanged()
                }

                override fun onNativeAdLoadFailed(adUnitId: String, error: MaxError) {
                    AppLovinAdUtils.nativeAdViewApplovinSmall = null
                    isNativeAdLoaded = false
                    Log.e("AppLovinNative", "Native Ad failed to load: ${error.message}")
                }

                override fun onNativeAdClicked(ad: MaxAd) {
                    Log.d("AppLovinNative", "Native Ad clicked")
                }
            })
            // Load the native ad with the custom layout
            val nativeAdView = AppLovinAdUtils.createNativeAdViewSmall(activity)
            AppLovinAdUtils.nativeAdLoadersmall?.loadAd(nativeAdView)
        }
        // Check if an ad is already loaded to avoid reloading

    }
}*/
