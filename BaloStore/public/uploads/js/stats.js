// Statistics page logic: charts, product list, filters, export
(function(){
	// Helper formatting
	function formatVnd(n){
		try{ return new Intl.NumberFormat('vi-VN').format(n) + ' ₫'; }catch(e){ return n + ' ₫'; }
	}

	// Sample product data
	const sampleProducts = [];
	for(let i=1;i<=23;i++){
		sampleProducts.push({
			id: i,
			name: `Sản phẩm mẫu ${i}`,
			qty: Math.floor(Math.random()*30)+1,
			price: (Math.floor(Math.random()*90)+10)*1000,
			size: [39,40,41,42][i%4],
			image: ''
		});
	}

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
		const labels = ['Hoàn thành','Đang vận chuyển','Chờ xác nhận','Đã hủy','Đã trả','Chờ giao hàng'];
		const data = [42,10,14,7,7,20];
		const colors = ['#16a34a','#f97316','#f43f5e','#374151','#60a5fa','#a78bfa'];

		if(statusChart) statusChart.destroy();
		statusChart = new Chart(ctx, {
			type: 'pie',
			data: { labels, datasets: [{ data, backgroundColor: colors }] },
			options: { responsive: true, plugins: { legend: { display: false } } }
		});

		// legend
		const legend = document.getElementById('statusLegend');
		legend.innerHTML = '';
		labels.forEach((l,i)=>{
			const r = document.createElement('div');
			r.className = 'flex items-center gap-2 text-sm mb-2';
			r.innerHTML = `<div style="width:14px;height:14px;background:${colors[i]};border-radius:4px;"></div><div>${l} <span class="text-gray-500">${data[i]}%</span></div>`;
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
		const data = generateRevenueData(range);
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
			tr.innerHTML = `<td class="p-3"><div class="no-image">IMG</div></td><td class="p-3 text-left">${p.name}</td><td class="p-3">${p.qty}</td><td class="p-3 price-column">${formatVnd(p.price)}</td><td class="p-3">${p.size}</td>`;
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
		// initial renders
		renderStatusChart();
		// ensure chart-type buttons reflect the current default
		document.querySelectorAll('.chart-type-btn').forEach(b=>{
			if(b.dataset.chartType === currentRevenueChartType) b.classList.add('bg-green-50'); else b.classList.remove('bg-green-50');
		});
		renderRevenueCharts('week', currentRevenueChartType);
		renderTopProducts();

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

