// Statistics page logic: charts, product list, filters, export
(function(){
	// Helper formatting
	function formatVnd(n){
		try{ return new Intl.NumberFormat('vi-VN').format(n) + ' ₫'; }catch(e){ return n + ' ₫'; }
	}

	// product data will be loaded from backend
	let sampleProducts = [];

	// Charts
	let statusChart = null;
	let revenueChart = null; // single chart instance for revenue (can be bar or line)
	let currentRevenueChartType = 'line'; // default to line view on startup

	function renderStatusChart(){
		const canvas = document.getElementById('statusChart');
		if (!canvas) { console.error('statusChart canvas not found'); return; }
		const ctx = canvas.getContext && canvas.getContext('2d');
		if (!ctx) { console.error('2D context for statusChart not available'); return; }
		if (typeof Chart === 'undefined') { console.error('Chart.js not loaded'); return; }
	// normalize and merge status labels to avoid duplicates and assign a unique color per normalized label
	let statusCountsRaw = (window.__statsData && window.__statsData.statusCounts) ? window.__statsData.statusCounts : null;
	let labels = [];
	let data = [];
	let bgColors = [];
	
	// Status translation map
	const statusTranslation = {
		'hoan_thanh': 'Hoàn thành',
		'hoàn_thành': 'Hoàn thành',
		'dang_van_chuyen': 'Đang vận chuyển',
		'đang_vận_chuyển': 'Đang vận chuyển',
		'cho_xac_nhan': 'Chờ xác nhận',
		'chờ_xác_nhận': 'Chờ xác nhận',
		'da_huy': 'Đã hủy',
		'đã_hủy': 'Đã hủy',
		'da_tra': 'Đã trả',
		'đã_trả': 'Đã trả',
		'cho_giao_hang': 'Chờ giao hàng',
		'chờ_giao_hàng': 'Chờ giao hàng',
		'chuyen_hoai': 'Chuyển hoàn',
		'dang_chuyen_hoai': 'Đang chuyển hoàn',
		'da_nhan_hang': 'Đã nhận hàng',
		'pending': 'Chờ xác nhận',
		'confirmed': 'Đã xác nhận',
		'processing': 'Đang xử lý',
		'shipped': 'Đã gửi',
		'delivered': 'Đã giao',
		'cancelled': 'Đã hủy',
		'returned': 'Đã trả'
	};
	
	if(statusCountsRaw && Array.isArray(statusCountsRaw)){
		// helper: remove diacritics and normalize
		function normalizeText(s){
			if(!s) return 'unknown';
			try{ return s.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase().trim(); }catch(e){ return String(s).toLowerCase().trim(); }
		}

		const agg = new Map(); // norm -> { display, count }
		for(const s of statusCountsRaw){
			if(!s) continue;
			const rawLabel = (s.status != null) ? String(s.status) : 'unknown';
			const norm = normalizeText(rawLabel) || 'unknown';
			const cnt = Number(s.count || 0);
			
			// Get translated label if available, otherwise use title case
			let displayLabel = statusTranslation[rawLabel.toLowerCase()] || statusTranslation[norm];
			if(!displayLabel){
				const parts = rawLabel.trim().split(/\s+/).map(p => p.charAt(0).toUpperCase() + p.slice(1).toLowerCase());
				displayLabel = parts.join(' ');
			}
			
			if(agg.has(norm)) {
				agg.get(norm).count += cnt;
			} else {
				agg.set(norm, { display: displayLabel, count: cnt });
			}
		}

		// color palette (expandable)
		const palette = ['#16a34a','#f97316','#baf508ff','#374151','#60a5fa','#a78bfa','#ee0606ff','#ef44e6ff','#10b981','#8b5cf6','#ef1515ff'];
		const colorByNorm = new Map();
		let colorIndex = 0;
		for(const [norm, v] of agg.entries()){
			labels.push(v.display);
			data.push(v.count);
			if(!colorByNorm.has(norm)){
				colorByNorm.set(norm, palette[colorIndex % palette.length]);
				colorIndex++;
			}
			bgColors.push(colorByNorm.get(norm));
		}
	} else {
		labels = ['Hoàn thành','Đang vận chuyển','Chờ xác nhận','Đã hủy','Đã trả','Chờ giao hàng'];
		data = [42,10,14,7,7,20];
		bgColors = ['#16a34a','#f97316','#baf508ff','#374151','#60a5fa','#a78bfa','#ee0606ff'];
	}
	const colors = ['#16a34a','#f97316','#baf508ff','#374151','#60a5fa','#a78bfa','#ee0606ff'];

		if(statusChart) statusChart.destroy();
		statusChart = new Chart(ctx, {
			type: 'pie',
			data: { labels, datasets: [{ data, backgroundColor: (bgColors.length? bgColors : colors) }] },
			options: { responsive: true, plugins: { legend: { display: false } } }
		});

		// legend with percentage and count
		const legend = document.getElementById('statusLegend');
		legend.innerHTML = '';
		const totalCount = data.reduce((s,n)=>s + (Number(n)||0), 0) || 1;
		labels.forEach((l,i)=>{
			const cnt = Number(data[i]||0);
			const pct = Math.round((cnt/totalCount)*100);
			const r = document.createElement('div');
			r.className = 'flex items-center gap-2 text-sm mb-2';
			r.innerHTML = `<div style="width:14px;height:14px;background:${colors[i % colors.length]};border-radius:4px;"></div><div>${l} <span class="text-gray-500">${pct}%</span> <span class="text-gray-400 ml-2">(${cnt})</span></div>`;
			legend.appendChild(r);
		});
	}

	function generateRevenueData(range){
		const labels=[]; const values=[];
		if(range==='day' || range==='week'){
			// last 7 days
			for(let i=6;i>=0;i--){
				const d = new Date(); d.setDate(d.getDate()-i);
				labels.push((d.getMonth()+1)+'/'+d.getDate());
				values.push(Math.floor(Math.random()*5000000)+200000);
			}
		} else if(range==='month'){
			// last 12 months
			const now = new Date();
			for(let i=11;i>=0;i--){
				const d = new Date(now.getFullYear(), now.getMonth()-i, 1);
				labels.push((d.getMonth()+1)+'/'+d.getFullYear());
				values.push(Math.floor(Math.random()*20000000)+1000000);
			}
		} else if(range==='year'){
			const y = new Date().getFullYear();
			for(let i=4;i>=0;i--){ labels.push(String(y-i)); values.push(Math.floor(Math.random()*200000000)+5000000); }
		} else {
			// custom default to 7 days
			return generateRevenueData('week');
		}
		return { labels, values };
	}

	function renderRevenueCharts(range, chartType){
		const data = (window.__statsData && window.__statsData.revenue) ? { labels: window.__statsData.revenue.map(r=>r.label), values: window.__statsData.revenue.map(r=>r.value) } : generateRevenueData(range);
		const type = chartType || currentRevenueChartType || 'bar';

		const canvas = document.getElementById('revenueLineChart');
		if (!canvas) { console.error('revenueLineChart canvas not found'); return; }
		const ctx = canvas && canvas.getContext && canvas.getContext('2d');
		if (!ctx) { console.error('2D context for revenue chart not available'); return; }
		if (typeof Chart === 'undefined') { console.error('Chart.js not loaded'); return; }

		if (revenueChart) revenueChart.destroy();

		const commonOptions = {
			responsive: true,
			scales: { y: { ticks: { callback: function(v){ return new Intl.NumberFormat('vi-VN').format(v); } } } }
		};

		if (type === 'bar'){
			revenueChart = new Chart(ctx, {
				type: 'bar',
				data: { labels: data.labels, datasets: [{ label: 'Doanh thu', data: data.values, backgroundColor: '#fb923c' }] },
				options: commonOptions
			});
		} else {
			revenueChart = new Chart(ctx, {
				type: 'line',
				data: { labels: data.labels, datasets: [{ label: 'Doanh thu', data: data.values, borderColor: '#0ea5a4', backgroundColor: 'rgba(14,165,164,0.08)', fill:true }] },
				options: commonOptions
			});
		}
	}

	// Top products table
	let currentPage = 1, pageSize = 5;
	function renderTopProducts(){
		const tbody = document.getElementById('topProductsBody');
		tbody.innerHTML = '';
		const start = (currentPage-1)*pageSize;
		const pageItems = sampleProducts.slice(start, start+pageSize);
		for(const p of pageItems){
			const tr = document.createElement('tr');
			const idx = start + pageItems.indexOf(p) + 1;
			// image cell
			let imgHtml = '';
			if(p.image){
				// try to use the image path directly; if relative, keep as-is
				imgHtml = `<img src="${p.image}" alt="" style="width:56px;height:56px;object-fit:cover;border-radius:6px;" />`;
			}else{
				imgHtml = `<div class="no-image" style="width:56px;height:56px;display:flex;align-items:center;justify-content:center;border:1px dashed #e5e7eb;border-radius:6px;color:#9ca3af">IMG</div>`;
			}
			const priceVal = p.price != null ? p.price : (p.revenue || 0);
			tr.innerHTML = `
				<td class="p-3">${idx}</td>
				<td class="p-3">${imgHtml}</td>
				<td class="p-3 text-left">${p.name || ''}</td>
				<td class="p-3 text-right">${formatVnd(priceVal)}</td>
				<td class="p-3 text-right">${p.qtySold || p.qty || 0}</td>
			`;
			tbody.appendChild(tr);
		}
		renderPagination();
	}

	function renderPagination(){
		const total = sampleProducts.length; const pages = Math.ceil(total/pageSize);
		const pWrap = document.getElementById('pagination'); pWrap.innerHTML='';
		for(let i=1;i<=pages;i++){
			const btn = document.createElement('button'); btn.className = 'px-3 py-1 border rounded ' + (i===currentPage? 'bg-orange-500 text-white':''); btn.textContent = i;
			btn.onclick = ()=>{ currentPage = i; renderTopProducts(); };
			pWrap.appendChild(btn);
		}
	}

	function exportToCsv(){
		const headers = ['id','name','qty','price','size'];
		const rows = sampleProducts.map(p=>[p.id,p.name,p.qty,p.price,p.size]);
		let csv = headers.join(',') + '\n' + rows.map(r=>r.map(v=>`"${String(v).replace(/"/g,'""')}"`).join(',')).join('\n');
		const blob = new Blob([csv],{type:'text/csv;charset=utf-8;'});
		const url = URL.createObjectURL(blob);
		const a = document.createElement('a'); a.href = url; a.download = 'products.csv'; document.body.appendChild(a); a.click(); a.remove(); URL.revokeObjectURL(url);
	}

	function setup(){
		// load stats from backend then render
		// build headers with Authorization if available in localStorage (legacy pages store bagistore_current_user)
		const _headers = { 'Accept': 'application/json' };
		try{
			const raw = localStorage.getItem('bagistore_current_user');
			if(raw){
				const parsed = JSON.parse(raw);
				const token = parsed && (parsed.token || parsed.accessToken || (parsed.data && parsed.data.token));
				if(token) _headers['Authorization'] = 'Bearer ' + token;
			}
		}catch(e){}

		// try same-origin first, fallback to explicit localhost:8080 for dev when needed
		const endpoints = ['/api/stats', 'http://localhost:8080/api/stats'];
		let lastErr = null;
		(async function(){
			let data = null;
			for(const url of endpoints){
				try{
					const resp = await fetch(url, { headers: _headers });
					if(!resp.ok) { lastErr = resp; continue; }
					data = await resp.json();
					break;
				}catch(err){ lastErr = err; }
			}
			if(!data){
				console.error('Failed to load stats', lastErr);
				// fallback render with sample data
				renderStatusChart();
				document.querySelectorAll('.chart-type-btn').forEach(b=>{ if(b.dataset.chartType === currentRevenueChartType) b.classList.add('bg-green-50'); else b.classList.remove('bg-green-50'); });
				renderRevenueCharts('week', currentRevenueChartType);
				renderTopProducts();
				return;
			}
			window.__statsData = data;
			// map topProducts to sampleProducts shape used by renderer (include price and image)
			sampleProducts = (data.topProducts || []).map(p => ({
				id: p.id,
				name: p.name,
				qtySold: p.qtySold,
				revenue: p.revenue,
				price: p.price,
				image: (p.image && typeof p.image === 'string') ? (p.image.startsWith('http') ? p.image : (p.image.startsWith('/') ? p.image : '/' + p.image)) : null
			}));

			// populate summary numbers
			try{
				const sOrders = document.getElementById('summaryOrders');
				if(sOrders) sOrders.textContent = data.totalOrders != null ? String(data.totalOrders) : '0';
				const sRev = document.getElementById('summaryRevenue');
				if(sRev) sRev.textContent = formatVnd(data.totalRevenue || 0);
				
				// populate actual revenue (from completed orders)
				const sActualRev = document.getElementById('summaryActualRevenue');
				if(sActualRev) sActualRev.textContent = formatVnd(data.actualRevenue || 0);
				
				// populate total revenue summary
				const sTotalRev = document.getElementById('summaryTotalRevenue');
				if(sTotalRev) sTotalRev.textContent = formatVnd(data.totalRevenue || 0);
			} catch(e){ console.error(e); }

			// populate KPI cards (expecting detailRows with labels: Hôm nay, Tuần này, Tháng này, Năm nay)
			if(Array.isArray(data.detailRows)){
				for(const row of data.detailRows){
					const label = (row.label || '').toLowerCase();
					const rev = row.revenue || 0;
					const prod = row.productsSold != null ? row.productsSold : (row.products != null ? row.products : 0);
					const orders = row.orders != null ? row.orders : 0;
					if(label.indexOf('hôm')>=0 || label.indexOf('hom')>=0){
						const elR = document.getElementById('kpiTodayRevenue'); if(elR) elR.textContent = formatVnd(rev);
						const elP = document.getElementById('kpiTodayProducts'); if(elP) elP.textContent = String(prod);
						const elO = document.getElementById('kpiTodayOrders'); if(elO) elO.textContent = String(orders);
					} else if(label.indexOf('tuần')>=0 || label.indexOf('tuan')>=0){
						const elR = document.getElementById('kpiWeekRevenue'); if(elR) elR.textContent = formatVnd(rev);
						const elP = document.getElementById('kpiWeekProducts'); if(elP) elP.textContent = String(prod);
						const elO = document.getElementById('kpiWeekOrders'); if(elO) elO.textContent = String(orders);
					} else if(label.indexOf('tháng')>=0 || label.indexOf('thang')>=0){
						const elR = document.getElementById('kpiMonthRevenue'); if(elR) elR.textContent = formatVnd(rev);
						const elP = document.getElementById('kpiMonthProducts'); if(elP) elP.textContent = String(prod);
						const elO = document.getElementById('kpiMonthOrders'); if(elO) elO.textContent = String(orders);
					} else if(label.indexOf('năm')>=0 || label.indexOf('nam')>=0){
						const elR = document.getElementById('kpiYearRevenue'); if(elR) elR.textContent = formatVnd(rev);
						const elP = document.getElementById('kpiYearProducts'); if(elP) elP.textContent = String(prod);
						const elO = document.getElementById('kpiYearOrders'); if(elO) elO.textContent = String(orders);
					}
				}

				// populate detail table
				const tbody = document.getElementById('statsDetailBody');
				if(tbody){
					tbody.innerHTML = '';
					for(const r of data.detailRows){
						const tr = document.createElement('tr');
						const growth = r.growth || '+';
						const growthCls = (String(growth).startsWith('-')) ? 'text-red-600' : 'text-green-600';
						tr.innerHTML = `<td class="p-3">${r.label}</td><td class="p-3 text-right">${formatVnd(r.revenue||0)}</td><td class="p-3 text-right">${r.orders||0}</td><td class="p-3 text-right">${formatVnd(r.avgPerOrder||0)}</td><td class="p-3 text-right ${growthCls}">${growth}</td>`;
						tbody.appendChild(tr);
					}
				}
			}
			// initial renders
			renderStatusChart();
			// ensure chart-type buttons reflect the current default
			document.querySelectorAll('.chart-type-btn').forEach(b=>{ if(b.dataset.chartType === currentRevenueChartType) b.classList.add('bg-green-50'); else b.classList.remove('bg-green-50'); });
			renderRevenueCharts('week', currentRevenueChartType);
			renderTopProducts();
		})();

		// time-range filter buttons (day/week/month/year/custom)
		document.querySelectorAll('.filter-btn').forEach(b=>{
			b.addEventListener('click', function(){
				document.querySelectorAll('.filter-btn').forEach(x=>x.classList.remove('bg-white'));
				this.classList.add('bg-white');
				const range = this.dataset.range || 'week';
				renderRevenueCharts(range, currentRevenueChartType);
			});
		});

		// chart-type buttons (line / bar)
		document.querySelectorAll('.chart-type-btn').forEach(b=>{
			b.addEventListener('click', function(){
				document.querySelectorAll('.chart-type-btn').forEach(x=>x.classList.remove('bg-green-50'));
				this.classList.add('bg-green-50');
				currentRevenueChartType = this.dataset.chartType || 'bar';
				// find active time range or default to week
				const activeTime = document.querySelector('.filter-btn.bg-white')?.dataset.range || 'week';
				renderRevenueCharts(activeTime, currentRevenueChartType);
			});
		});

		document.getElementById('pageSize').addEventListener('change', function(){ pageSize = Number(this.value||5); currentPage=1; renderTopProducts(); });
		document.getElementById('exportBtn').addEventListener('click', exportToCsv);
	}

	// Call setup when DOM is ready. If DOMContentLoaded already fired, run immediately.
	function onReady(fn){
		if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', fn);
		else { try { fn(); } catch(e) { console.error(e); } }
	}
	onReady(setup);

})();

