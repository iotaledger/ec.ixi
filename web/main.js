let $input_seed;
let $wallet_table;
let $cluster_table;
let $actors_table;

window.onload = function () {
    init_elements();
};

/**
 * Initializes global jQuery pointers to commonly used DOM objects.
 * */
function init_elements() {
    $wallet_table = $('#wallet table');
    $cluster_table = $('#cluster table');
    $actors_table = $('#actors table');
    $input_seed = $('#seed');
}

/**
 * Loads and displays the addresses with their respective balances in the wallet section.
 * */
function refresh_wallet() {
    const balances = get_balances($input_seed.val());
    $('#wallet table').html(gen_table_row(["address", "balance"], true));
    $.each(balances, function (address, balance) {
        $wallet_table.append(gen_table_row([address, balance]));
    });
}

/**
 * @param cells array of row's cell contents in correct order
 * @param th if enabled will use <th> cells instead of <td>
 * @return table row whose cells are filled with respective content
 * */
function gen_table_row(cells, th = false) {
    const $tr = $("<tr>");
    $.each(cells, function (_, cell) {
        $tr.append($(th ? "<th>" : "<td>").text(cell));
    });
    return $tr;
}

/**
 * @param seed The seed to derive the addresses from.
 * @return A map of the addresses derived from this seed with their respective balances in iotas.
 * */
function get_balances(seed) {
    const balance_by_address = {
        "KDNMYEMGVXEGJS9MSLV9AE9VYCLOGKTCOUZDUDTS99K9SNSLWYGMCWYWTYXJEY9ADMH9AISGQL9IA9999": 117,
        "E9VYCLOGKTCOUZDUDTS99KKDNMYEMGVXEGJS9MSLV9A9SNSLWYGMCWYWTYXJEY9ADMH9AISGQL9IA9999": 33
    };
    return balance_by_address;
}