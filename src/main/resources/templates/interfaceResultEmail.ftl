<!DOCTYPE html>
<html>
<body>
	<style type="text/css">
		.tg {
			border-collapse: collapse;
			border-spacing: 0;
			border-color: #999;
		}

		.tg td {
			font-family: Arial, sans-serif;
			font-size: 14px;
			padding: 10px 5px;
			overflow: hidden;
			word-break: normal;
			border: 1px solid #999;
			color: #444;
			background-color: #F7FDFA;
		}

		.tg th {
			font-family: Arial, sans-serif;
			font-size: 14px;
			font-weight: normal;
			padding: 10px 5px;
			overflow: hidden;
			word-break: normal;
			border: 1px solid #999;
			color: #2c3e50;
			background-color: #ecf0f1;
		}

		.tg .tg-yw4l {
			vertical-align: top
		}

		.tg .tg-mhzi{background-color:#ecf0f1;color:#2c3e50;vertical-align:top}

		.number { text-align: center; }
	</style>
	<h1>Interface-driven search result</h1>
	<h2>Query: ${query}</h2>
	<table class="tg">
		<tr>
			<th class="tg-yw4l" rowspan="2">Component</th>
			<th class="tg-yw4l" rowspan="2">FQ Name</th>
			<th class="tg-yw4l" colspan="${numMetrics}">Ranking Criteria</th>
			<th class="tg-yw4l" rowspan="2">Version</th>
			<th class="tg-yw4l" rowspan="2">Description</th>
		</tr>
		<tr>
		<#list metrics as metric>
			<td class="tg-mhzi">${metric.title}</td>
		</#list>
		</tr>
		<#list candidates as candidate>
		<tr>
			<td class="tg-yw4l">${candidate.name}</td>
			<td class="tg-yw4l">${candidate.fqName}</td>
			<#list candidate.metrics as metric>
				<td class="tg-yw4l number">${metric.value}</td>
			</#list>
			<td class="tg-yw4l">${candidate.version}</td>
			<td class="tg-yw4l">${candidate.description}</td>
		</tr>
		</#list>
	</table>
</body>
</html>