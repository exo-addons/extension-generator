(function($) {
	$('.title-commands #expandAllTree').on("click", function() {
		$('.uiTreeExplorer .tree-checkbox').each(function() {
			this.checked = true;
		});
	});
	$('.title-commands #collapseAllTree').on("click", function() {
		$('.uiTreeExplorer .tree-checkbox').each(function() {
			this.checked = false;
		});
	});
	$('.title-commands #controlAll').on(
			"change",
			function() {
				var checked = $(this).attr("checked");
				$('.uiTreeExplorer .list-checkbox.parent').each(function() {
					this.checked = checked;
					fireCheckBoxChange(this);
				});
				$('#exportImportForm').jzLoad(
						"ExtensionGeneratorController.selectResources()", {
							"path" : "",
							"checked" : ""
						});
			});

	$(document).ready(function() {
		$.fn.DataTable = jQuery.fn.dataTable;
		$.fn.dataTable = jQuery.fn.dataTable;
		$.fn.dataTableSettings = jQuery.fn.dataTable.settings;
		$.fn.dataTableExt = jQuery.fn.dataTable.ext;

		$('.tree_datatable').each(function() {
			$(this).dataTable({
				"bSort" : false,
				"aaSorting" : [],
				"aoColumns" : [ {
					"bSortable" : false
				} ],
				"bFilter" : true,
				"bPaginate" : false,
				"bInfo" : false,
				"sScrollY" : 252,
				"oLanguage" : {
					"sSearch" : "",
					"oSearch" : false
				}
			});
		});
	});
	function fireCheckBoxChange(obj) {
		if (!obj || !obj.id) {
			obj = this;
		}
		var checkboxId = $(obj).attr("id");
		var checked = $(obj).attr('checked')
				&& ($(obj).attr('checked') == 'checked');
		if (!checked) {
			checked = false;
		}

		var childrenChecked = false;
		var allChildrenChecked = true;
		$(obj).closest('li').find('.list-checkbox').each(function() {
			childrenChecked = childrenChecked || this.checked;
			allChildrenChecked = allChildrenChecked && this.checked;
		});

		$(obj).closest('ul').closest('li').children('.list-checkbox')
				.each(
						function() {
							this.checked = allChildrenChecked;
							this.indeterminate = childrenChecked
									&& !allChildrenChecked;
						});

		// Select all sub checkboxes
		if ($(obj).hasClass("parent")) {
			$(obj).closest('.node').find('.list-checkbox').each(function() {
				this.checked = checked;
			});
		} else if ($(obj).hasClass("leaf")) {
			var childrenChecked = false;
			var allChildrenChecked = true;
			$(obj).closest('.node').find('.list-checkbox').each(function() {
				childrenChecked = childrenChecked || this.checked;
				allChildrenChecked = allChildrenChecked && this.checked;
			});
			$(obj).closest('.nodeGroup').closest('.node').find(
					'input[type="checkbox"].parent').each(function() {
				this.checked = allChildrenChecked;
				this.indeterminate = childrenChecked && !allChildrenChecked;
			});
		}
		$('#exportImportForm').jzLoad(
				"ExtensionGeneratorController.selectResources()", {
					"path" : checkboxId,
					"checked" : checked
				});
	}
	$('#extension-genrator-portlet .list-checkbox').on("change",
			fireCheckBoxChange);
	window.exportProject = function() {
		var extensionNameValue = $('#extensionName').val();
		var archiveTypeValue = $('.archiveTypeContainer input:radio[name=archiveType]:checked').val();
		if(!extensionNameValue || extensionNameValue == "") {
			$('#extensionName').css("border-color", "red");
			return;
		}
		var re = /[a-z|A-Z]*[-|_]*[a-z|A-Z]*/;
		var match = extensionNameValue.match(re);
		if (match == null || match[0] != extensionNameValue) {
			$('#extensionName').css("border-color", "red");
			return;
		}
		$('#extensionName').removeAttr("style");
		window.location.href = $('#exportImportForm').jzURL('ExtensionGeneratorController.exportExtension') + "&extensionName=" + extensionNameValue + "&archiveType=" + archiveTypeValue;
	}
})($);