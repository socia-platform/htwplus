$(document).ready(function () {
	
	function reloadNotifications()  {
		$.ajax({
			url: "/notification/view",
			type: "GET",
			success: function(data){
				var setOpen = false;
				if($("#hp-notification-item").hasClass("open")){
					setOpen = true;
				}
				$("#hp-notification-item").replaceWith(data)
				
				if(setOpen == true) {
					$("#hp-notification-item").addClass("open");
				}

			}
		});
	}
	
	function deleteNotifications() {
		$.ajax({
			url: "/notification/delete",
			type: "GET",
			success: function(data){
				reloadNotifications(); 		
			}
		});
		
	}
	
	$('body').on("click", "#hp-delete-notifications", function(){
		deleteNotifications();
		return false;
	});
	
	setInterval( reloadNotifications, 30000 );
	
});