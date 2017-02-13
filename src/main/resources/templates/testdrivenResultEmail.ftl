<!DOCTYPE html>
<html>
<head>
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
			border-style: solid;
			border-width: 1px;
			overflow: hidden;
			word-break: normal;
			border-color: #999;
			color: #444;
			background-color: #F7FDFA;
		}

		.tg th {
			font-family: Arial, sans-serif;
			font-size: 14px;
			font-weight: normal;
			padding: 10px 5px;
			border-style: solid;
			border-width: 1px;
			overflow: hidden;
			word-break: normal;
			border-color: #999;
			color: #2c3e50;
			background-color: #ecf0f1;
		}

		.tg .tg-yw4l {
			vertical-align: top
		}
	</style>
</head>
<body>
	<h1>Test-driven search result</h1>
	<h2>Job ID: ${jobId}</h2>
	<h2>SOCORA Link: http://socora.merobase.com/socora/app/index.html#?result_hash=${jobId}</h2>
	<table class="tg">
		<tr>
			<th class="tg-yw4l">Component</th>
			<th class="tg-yw4l">FQ Name</th>
			<th class="tg-yw4l" colspan="3">Functional Requirements</th>
			<th class="tg-yw4l">Version</th>
			<th class="tg-yw4l">Description</th>
		</tr>
	<#list candidates as candidate>
		<tr>
			<td class="tg-yw4l">${candidate.name}</td>
			<td class="tg-yw4l">${candidate.fqName}</td>
			<#list candidate.metrics as metric>
				<td class="tg-yw4l">${metric}</td>
			</#list>
			<td class="tg-yw4l">${candidate.version}</td>
			<td class="tg-yw4l">${candidate.description}</td>
		</tr>
	</#list>
	</table>
</body>
</html>